package com.github.dbmdz.flusswerk.framework.rabbitmq;

import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
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

  private final RoutingProperties routingConfig;

  private final RabbitClient rabbitClient;

  public MessageBroker(RoutingProperties routing, RabbitClient rabbitClient) throws IOException {
    this.routingConfig = routing;
    this.rabbitClient = rabbitClient;

    provideExchanges();
    provideInputQueues();
    provideOutputQueues();
  }

  /**
   * Sends a message to the default output queue as JSON document.
   *
   * @param message the message to send.
   * @throws IOException if sending the message fails.
   * @deprecated Use {@link Topic#send(Message)} instead
   */
  @Deprecated
  void send(Message message) throws IOException {
    var topic = routingConfig.getOutgoing().get("default");
    if (topic == null) {
      throw new RuntimeException("Cannot send message, no default queue specified");
    }
    send(topic, message);
  }

  /**
   * Sends messages to the default output queue as JSON document.
   *
   * @param messages the message to send.
   * @throws IOException if sending the message fails.
   * @deprecated Use {@link Topic#send(Message)} instead
   */
  @Deprecated
  public void send(Collection<? extends Message> messages) throws IOException {
    var topic = routingConfig.getOutgoing().get("default");
    if (topic == null) {
      throw new RuntimeException("Cannot send messages, no default queue specified");
    }
    send(topic, messages);
  }

  /**
   * Sends a message to a certain queue as JSON document.
   *
   * @param routingKey the routing key for the queue to send the message to (usually the queue
   *     name).
   * @param message the message to send.
   * @throws IOException if sending the message fails.
   */
  void send(String routingKey, Message message) throws IOException {
    rabbitClient.send(routingConfig.getExchange(routingKey), routingKey, message);
  }

  void sendRaw(String routingKey, byte[] message) {
    rabbitClient.sendRaw(routingConfig.getExchange(routingKey), routingKey, message);
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
  void send(String routingKey, Collection<? extends Message> messages) throws IOException {
    for (Message message : messages) {
      send(routingKey, message);
    }
  }

  /**
   * Gets one message from the queue but does not acknowledge it. To do so, use {@link
   * MessageBroker#ack(Message)}.
   *
   * @param queueName the queue to receive.
   * @return the received message.
   * @throws InvalidMessageException if the message could not be read and deserialized
   */
  public Message receive(String queueName) throws InvalidMessageException {
    return receive(queueName, false);
  }

  /**
   * Gets one message from the queue. If specified, the message will be acknowledged automatically.
   *
   * @param queueName the queue to receive.
   * @param autoAck whether the message should be acknowledged automatically.
   * @return the received message.
   * @throws InvalidMessageException if the message could not be read and deserialized
   */
  public Message receive(String queueName, boolean autoAck) throws InvalidMessageException {
    return rabbitClient.receive(queueName, autoAck);
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
    for (String inputQueue : routingConfig.getIncoming()) {
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

  private void failInvalidMessage(InvalidMessageException e) {
    Envelope envelope = e.getEnvelope();
    LOGGER.warn("Invalid message detected. Will be shifted into 'failed' queue: " + e.getMessage());
    rabbitClient.ack(envelope);
    FailurePolicy failurePolicy = routingConfig.getFailurePolicy(envelope.getSource());
    String failedRoutingKey = failurePolicy.getFailedRoutingKey();
    if (failedRoutingKey != null) {
      rabbitClient.sendRaw(
          routingConfig.getExchange(failedRoutingKey),
          failedRoutingKey,
          envelope.getBody().getBytes());
    }
  }

  private void provideInputQueues() throws IOException {
    for (String inputQueue : routingConfig.getIncoming()) {
      FailurePolicy failurePolicy = routingConfig.getFailurePolicy(inputQueue);
      final String exchange = routingConfig.getExchange(inputQueue);
      final String deadLetterExchange = routingConfig.getDeadLetterExchange(inputQueue);
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

  private void provideOutputQueues() throws IOException {
    for (String topic : routingConfig.getOutgoing().values()) {
      rabbitClient.declareQueue(
          topic,
          routingConfig.getExchange(topic),
          topic,
          Map.of(DEAD_LETTER_EXCHANGE, routingConfig.getDeadLetterExchange(topic)));
    }
  }

  /**
   * Acknowledges a message to remove it from the queue.
   *
   * @param message the message to acknowledge.
   */
  public void ack(Message message) {
    rabbitClient.ack(message.getEnvelope());
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
    final long maxRetries = routingConfig.getFailurePolicy(message).getRetries();
    if (envelope.getRetries() < maxRetries) {
      envelope.setRetries(envelope.getRetries() + 1);
      retry(message);
      return true;
    } else {
      fail(message, false); // Avoid double ACKing the origin message
      return false;
    }
  }

  void fail(Message message, boolean ackMessage) throws IOException {
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

  public void retry(Message message) throws IOException {
    LOGGER.debug("Send message to retry queue: " + message);
    FailurePolicy failurePolicy = routingConfig.getFailurePolicy(message);
    String retryRoutingKey = failurePolicy.getRetryRoutingKey();
    if (retryRoutingKey != null) {
      String queue = message.getEnvelope().getSource();
      String exchange = routingConfig.getDeadLetterExchange(queue);
      rabbitClient.send(exchange, queue, message);
    }
  }

  private void provideExchanges() throws IOException {
    for (String exchange : routingConfig.getExchanges()) {
      rabbitClient.provideExchange(exchange);
    }
    for (String deadLetterExchange : routingConfig.getDeadLetterExchanges()) {
      rabbitClient.provideExchange(deadLetterExchange);
    }
  }

  /**
   * Returns the number of messages in known queues
   *
   * @return a map of queue names and the number of messages in these queues
   * @throws IOException if communication with RabbitMQ fails
   * @deprecated Use {@link Queue#messageCount()} instead.
   */
  @Deprecated
  Map<String, Long> getMessageCounts() throws IOException {
    Map<String, Long> result = new HashMap<>();
    for (String queue : routingConfig.getIncoming()) {
      result.put(queue, rabbitClient.getMessageCount(queue));
    }
    return result;
  }

  Map<String, Long> getFailedMessageCounts() throws IOException {
    Map<String, Long> result = new HashMap<>();
    for (String inputQueue : routingConfig.getIncoming()) {
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
    for (String inputQueue : routingConfig.getIncoming()) {
      FailurePolicy failurePolicy = routingConfig.getFailurePolicy(inputQueue);
      if (failurePolicy != null) {
        String queue = failurePolicy.getRetryRoutingKey();
        result.put(queue, rabbitClient.getMessageCount(queue));
      }
    }
    return result;
  }
}
