package com.github.dbmdz.flusswerk.integration;

import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.FailurePolicy;
import com.github.dbmdz.flusswerk.framework.rabbitmq.Queue;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  public String firstInput() {
    return routing.getIncoming().get(0);
  }

  public String output() {
    return routing.getOutgoing().get("default").get(0);
  }

  public void send(Message message) throws IOException {
    rabbitMQ.topic(firstInput()).send(message);
  }

  private Message checkedReceive(String queueName, boolean autoAck)
      throws InvalidMessageException, InterruptedException {
    var queue = rabbitMQ.queue(queueName);
    Thread.sleep(10); // wait for message to arrive
    Optional<Message> received = queue.receive(autoAck);
    if (received.isEmpty()) {
      Thread.sleep(2000); // wait more in case first wait was not enough
      received = queue.receive(autoAck);
    }
    if (received.isEmpty()) {
      throw new RuntimeException("No message received");
    }
    Message message = received.get();
    if (!autoAck) {
      rabbitMQ.ack(message);
    }
    return message;
  }

  public Message receive() {
    return receive(false);
  }

  public Message receive(boolean autoAck) {
    try {
      return checkedReceive(output(), autoAck);
    } catch (InterruptedException | InvalidMessageException e) {
      throw new RuntimeException(e);
    }
  }

  public Message receiveFailed() {
    return receiveFailed(false);
  }

  public Message receiveFailed(boolean autoAck) {
    String failedQueue = routing.getFailurePolicy(firstInput()).getFailedRoutingKey();
    try {
      return checkedReceive(failedQueue, autoAck);
    } catch (InterruptedException | InvalidMessageException e) {
      throw new RuntimeException(e);
    }
  }

  public FailurePolicy firstFailurePolicy() {
    return routing.getFailurePolicy(firstInput());
  }

  public void purgeQueues() {
    for (Queue queue : allQueues()) {
      var deletedMessages = queue.purge();
      if (deletedMessages != 0) {
        LOGGER.error("Purged {} and found {} messages.", queue, deletedMessages);
      }
    }
  }

  public List<Queue> allQueues() {
    Stream<String> queueNames = routing.getIncoming().stream();
    queueNames = Stream.concat(queueNames, routing.allOutgoing().stream());
    queueNames =
        Stream.concat(
            queueNames,
            routing.getIncoming().stream()
                .map(routing::getFailurePolicy)
                .map(FailurePolicy::getFailedRoutingKey));
    queueNames =
        Stream.concat(
            queueNames,
            routing.getIncoming().stream()
                .map(routing::getFailurePolicy)
                .map(FailurePolicy::getRetryRoutingKey));
    return queueNames.map(rabbitMQ::queue).collect(Collectors.toList());
  }
}
