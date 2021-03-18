package com.github.dbmdz.flusswerk.integration;

import static org.junit.jupiter.api.Assertions.fail;

import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.FailurePolicy;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(RabbitUtil.class);

  private final RabbitMQ rabbitMQ;

  private final RoutingProperties routing;

  public RabbitUtil(RabbitMQ rabbitMQ, RoutingProperties routing) {
    this.rabbitMQ = rabbitMQ;
    this.routing = routing;
  }

  public Message waitForMessage(String queueName, FailurePolicy failurePolicy, String testName)
      throws InterruptedException, IOException, InvalidMessageException {
    var queue = rabbitMQ.queue(queueName);
    var received = queue.receive();
    var attempts = 0;
    while (received.isEmpty()) {
      if (attempts > 50) {
        fail("Too many attempts to receive message");
      }
      Thread.sleep(failurePolicy.getBackoff().toMillis()); // dead letter backoff time is 1s
      received = queue.receive();
      attempts++;
      LOGGER.info(
          "{}: Receive message attempt {}, got {} ({} retry, {} failed)",
          testName,
          attempts,
          received.isPresent() ? "message" : "nothing",
          rabbitMQ.queue(failurePolicy.getRetryRoutingKey()).messageCount(),
          rabbitMQ.queue(failurePolicy.getFailedRoutingKey()).messageCount());
    }
    return received.get();
  }

  public void purgeQueues() throws IOException {
    // Cleanup leftover messages to not pollute other tests
    var readFrom = routing.getIncoming();
    for (String queue : readFrom) {
      purge(queue);
      var failurePolicy = routing.getFailurePolicy(queue);
      purge(failurePolicy.getFailedRoutingKey()); // here routing key == queue name
      purge(failurePolicy.getRetryRoutingKey()); // here routing key == queue name
    }

    var writeTo = routing.getOutgoing();
    for (String queue : writeTo.values()) {
      purge(queue);
    }
  }

  private void purge(String queue) throws IOException {
    var deletedMessages = rabbitMQ.queue(queue).purge();
    if (deletedMessages != 0) {
      LOGGER.error("Purged {} and found {} messages.", queue, deletedMessages);
    }
  }

  public Message waitAndAck(String queueName, Duration backoff)
      throws IOException, InvalidMessageException, InterruptedException {
    var queue = rabbitMQ.queue(queueName);
    var received = queue.receive();
    var attempts = 0;
    while (received.isEmpty()) {
      if (attempts > 50) {
        fail("Too many attempts to receive message");
      }
      Thread.sleep(backoff.toMillis()); // dead letter backoff time is 1s
      received = queue.receive();
      attempts++;
    }
    rabbitMQ.ack(received.get());
    return received.get();
  }
}
