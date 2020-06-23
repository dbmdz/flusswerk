package com.github.dbmdz.flusswerk.framework.messagebroker;

import com.github.dbmdz.flusswerk.framework.config.properties.Routing;
import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.model.Envelope;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MessageBroker provides a high level API to interact with an MessageBroker like RabbitMQ and
 * provides the framework engines logic for message operations like sending, retrieving or rejecting
 * for messages.
 */
public class MessageBroker {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageBroker.class);
  private static final String MESSAGE_TTL = "x-message-ttl";
  private static final String DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";

  private final Routing routingConfig;

  private final RabbitClient rabbitClient;

  public MessageBroker(Routing routing, RabbitClient rabbitClient) throws IOException {
    this.routingConfig = routing;
    this.rabbitClient = rabbitClient;

    provideExchanges();
    provideInputQueues();
    if (routingConfig.getWriteTo().isPresent()) {
      provideOutputQueue();
    }
  }

  /**
   * Sends a message to the default output queue as JSON document.
   *
   * @param message the message to send.
   * @throws IOException if sending the message fails.
   */
  public void send(Message message) throws IOException {
    send(routingConfig.getWriteTo().orElseThrow(), message);
  }

  /**
   * Sends messages to the default output queue as JSON document.
   *
   * @param messages the message to send.
   * @throws IOException if sending the message fails.
   */
  public void send(Collection<? extends Message> messages) throws IOException {
    send(routingConfig.getWriteTo().orElseThrow(), messages);
  }

  /**
   * Sends a message to a certain queue as JSON document.
   *
   * @param routingKey the routing key for the queue to send the message to (usually the queue
   *     name).
   * @param message the message to send.
   * @throws IOException if sending the message fails.
   */
  public void send(String routingKey, Message message) throws IOException {
    rabbitClient.send(routingConfig.getExchange(), routingKey, message);
  }

  /**
   * Sends multiple messages to a certain queue as JSON documents. The messages are sent in the same
   * order as returned by the iterator over <code>messages</code>.
   *
   * @param routingKey the routing key for the queue to send the message to (usually the queue
   *     name).
   * @param messages the messages to send.
   * @throws IOException if sending a message fails.
   */
  public void send(String routingKey, Collection<? extends Message> messages) throws IOException {
    for (Message message : messages) {
      send(routingKey, message);
    }
  }

  public void sendRaw(String routingKey, Message message) throws IOException {
    rabbitClient.sendRaw(
        routingConfig.getExchange(), routingKey, message.getEnvelope().getBody().getBytes());
  }

  /**
   * Gets one message from the queue but does not acknowledge it. To do so, use {@link
   * MessageBroker#ack(Message)}.
   *
   * @param queueName the queue to receive.
   * @return the received message.
   * @throws IOException if communication with RabbitMQ failed.
   * @throws InvalidMessageException if the message could not be read and deserialized
   */
  public Message receive(String queueName) throws IOException, InvalidMessageException {
    return rabbitClient.receive(queueName);
  }

  /**
   * Gets one message from the input queue but does not acknowledge it. To do so, use {@link
   * MessageBroker#ack(Message)}.
   *
   * @return the received message.
   * @throws IOException if communication with RabbitMQ failed.
   */
  public Message receive() throws IOException {
    Message message = null;
    for (String inputQueue : routingConfig.getReadFrom()) {
      try {
        message = receive(inputQueue);
      } catch (InvalidMessageException e) {
        failInvalidMessage(e);
        return null;
      }

      if (message != null) {
        break;
      }
    }
    return message;
  }

  private void failInvalidMessage(InvalidMessageException e) throws IOException {
    Message message = e.getInvalidMessage();
    LOGGER.warn("Invalid message detected. Will be shifted into 'failed' queue: " + e.getMessage());
    failRawWithAck(message);
  }

  private void provideInputQueues() throws IOException {
    final String deadLetterExchange = routingConfig.getDeadLetterExchange();
    final String exchange = routingConfig.getExchange();

    for (String inputQueue : routingConfig.getReadFrom()) {
      FailurePolicy failurePolicy = routingConfig.getFailurePolicy(inputQueue);
      rabbitClient.declareQueue(
          inputQueue, exchange, inputQueue, Map.of(DEAD_LETTER_EXCHANGE, deadLetterExchange));
      if (failurePolicy.getRetryRoutingKey() != null) {
        rabbitClient.declareQueue(
            failurePolicy.getRetryRoutingKey(),
            deadLetterExchange,
            inputQueue,
            Map.of(
                MESSAGE_TTL,
                failurePolicy.getBackoff().toMillis(),
                DEAD_LETTER_EXCHANGE,
                exchange));
      }
      if (failurePolicy.getFailedRoutingKey() != null) {
        rabbitClient.declareQueue(
            failurePolicy.getFailedRoutingKey(),
            exchange,
            failurePolicy.getFailedRoutingKey(),
            null);
      }
    }
  }

  private void provideOutputQueue() throws IOException {
    var topic = routingConfig.getWriteTo().orElseThrow();
    rabbitClient.declareQueue(
        topic,
        routingConfig.getExchange(),
        topic,
        Map.of(DEAD_LETTER_EXCHANGE, routingConfig.getDeadLetterExchange()));
  }

  /**
   * Acknowledges a message to remove it from the queue.
   *
   * @param message the message to acknowledge.
   * @throws IOException if communication with RabbitMQ failed.
   */
  public void ack(Message message) throws IOException {
    rabbitClient.ack(message);
  }

  /**
   * Rejects a messaging and takes care of proper dead lettering, retries and, if the message failed
   * too often, routing to the failed queue.
   *
   * @param message the message to reject
   * @return true if retry, false if failed
   * @throws IOException if communication with RabbitMQ failed
   */
  public boolean reject(Message message) throws IOException {
    final Envelope envelope = message.getEnvelope();
    final long maxRetries = routingConfig.getFailurePolicy(message).getMaxRetries();
    ack(message);
    if (envelope.getRetries() < maxRetries) {
      envelope.setRetries(envelope.getRetries() + 1);
      retry(message);
      return true;
    } else {
      fail(message, false); // Avoid double ACKing the origin message
      return false;
    }
  }

  public void failRawWithAck(Message message) throws IOException {
    ack(message);
    LOGGER.debug("Send message to failed queue: " + message);
    FailurePolicy failurePolicy = routingConfig.getFailurePolicy(message);
    String failedRoutingKey = failurePolicy.getFailedRoutingKey();
    if (failedRoutingKey != null) {
      sendRaw(failedRoutingKey, message);
    }
  }

  public void fail(Message message, boolean ackMessage) throws IOException {
    if (ackMessage) {
      ack(message);
    }
    LOGGER.debug("Send message to failed queue: " + message);
    FailurePolicy failurePolicy = routingConfig.getFailurePolicy(message);
    String failedRoutingKey = failurePolicy.getFailedRoutingKey();
    if (failedRoutingKey != null) {
      send(failedRoutingKey, message);
    }
  }

  public void fail(Message message) throws IOException {
    fail(message, true);
  }

  private void retry(Message message) throws IOException {
    LOGGER.debug("Send message to retry queue: " + message);
    FailurePolicy failurePolicy = routingConfig.getFailurePolicy(message);
    String retryRoutingKey = failurePolicy.getRetryRoutingKey();
    if (retryRoutingKey != null) {
      rabbitClient.send(
          routingConfig.getDeadLetterExchange(), message.getEnvelope().getSource(), message);
    }
  }

  private void provideExchanges() throws IOException {
    rabbitClient.provideExchange(routingConfig.getExchange());
    rabbitClient.provideExchange(routingConfig.getDeadLetterExchange());
  }

  public Map<String, Long> getMessageCounts() throws IOException {
    Map<String, Long> result = new HashMap<>();
    for (String queue : routingConfig.getReadFrom()) {
      result.put(queue, rabbitClient.getMessageCount(queue));
    }
    return result;
  }

  public Map<String, Long> getFailedMessageCounts() throws IOException {
    Map<String, Long> result = new HashMap<>();
    for (String inputQueue : routingConfig.getReadFrom()) {
      FailurePolicy failurePolicy = routingConfig.getFailurePolicy(inputQueue);
      if (failurePolicy != null) {
        String queue = failurePolicy.getFailedRoutingKey();
        result.put(queue, rabbitClient.getMessageCount(queue));
      }
    }
    return result;
  }

  public Map<String, Long> getRetryMessageCounts() throws IOException {
    Map<String, Long> result = new HashMap<>();
    for (String inputQueue : routingConfig.getReadFrom()) {
      FailurePolicy failurePolicy = routingConfig.getFailurePolicy(inputQueue);
      if (failurePolicy != null) {
        String queue = failurePolicy.getRetryRoutingKey();
        result.put(queue, rabbitClient.getMessageCount(queue));
      }
    }
    return result;
  }
}
