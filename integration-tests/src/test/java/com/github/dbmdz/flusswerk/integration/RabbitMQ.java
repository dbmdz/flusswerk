package com.github.dbmdz.flusswerk.integration;

import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.rabbitmq.FailurePolicy;
import com.github.dbmdz.flusswerk.framework.rabbitmq.MessageBroker;
import com.github.dbmdz.flusswerk.framework.rabbitmq.Queues;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitMQ {

  private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQ.class);

  private final MessageBroker messageBroker;

  private final Queues queues;

  private final RoutingProperties routing;

  public RabbitMQ(MessageBroker messageBroker, Queues queues, RoutingProperties routing) {
    this.messageBroker = messageBroker;
    this.queues = queues;
    this.routing = routing;
  }

  public Message waitForMessage(String queue, FailurePolicy failurePolicy, String testName)
      throws InterruptedException, IOException, InvalidMessageException {
    var received = messageBroker.receive(queue);
    var attempts = 0;
    while (received == null) {
      if (attempts > 50) {
        Assert.fail("To many attempts to receive message");
      }
      Thread.sleep(failurePolicy.getBackoff().toMillis()); // dead letter backoff time is 1s
      received = messageBroker.receive(queue);
      attempts++;
      LOGGER.info(
          "{}: Receive message attempt {}, got {} ({} retry, {} failed)",
          testName,
          attempts,
          received != null ? "message" : "nothing",
          queues.messageCount(failurePolicy.getRetryRoutingKey()),
          queues.messageCount(failurePolicy.getFailedRoutingKey()));
    }
    return received;
  }

  public void purgeQueues() throws IOException {
    // Cleanup leftover messages to not pollute other tests
    var readFrom = routing.getReadFrom();
    for (String queue : readFrom) {
      purge(queue);
      var failurePolicy = routing.getFailurePolicy(queue);
      purge(failurePolicy.getFailedRoutingKey()); // here routing key == queue name
      purge(failurePolicy.getRetryRoutingKey()); // here routing key == queue name
    }

    var writeTo = routing.getWriteTo();
    if (writeTo.isPresent()) {
      purge(writeTo.get());
    }
  }

  private void purge(String queue) throws IOException {
    var deletedMessages = queues.purge(queue);
    if (deletedMessages != 0) {
      LOGGER.error("Purged {} and found {} messages.", queue, deletedMessages);
    }
  }
}
