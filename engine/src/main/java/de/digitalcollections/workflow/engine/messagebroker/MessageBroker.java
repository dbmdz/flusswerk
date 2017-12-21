package de.digitalcollections.workflow.engine.messagebroker;

import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;
import de.digitalcollections.workflow.engine.model.Meta;
import de.digitalcollections.workflow.engine.util.Maps;
import java.io.IOException;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MessageBroker provides a high level API to interact with an MessageBroker like RabbitMQ and provides the framework engines logic for message operations like sending, retrieving or rejecting for messages.
 */
public class MessageBroker {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageBroker.class);
  private static final String MESSAGE_TTL = "x-message-ttl";
  private static final String DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";

  private MessageBrokerConfig config;

  private RoutingConfig routingConfig;

  private final RabbitClient rabbitClient;

  MessageBroker(MessageBrokerConfig config, RoutingConfig routingConfig, RabbitClient rabbitClient) throws IOException {
    this.config = config;
    this.routingConfig = routingConfig;
    this.rabbitClient = rabbitClient;

    provideExchanges();
    provideInputQueues();
    if (routingConfig.hasWriteTo()) {
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
    rabbitClient.send(routingConfig.getExchange(), routingConfig.getWriteTo(), message);
  }

  /**
   * Sends a message to a certain queue as JSON document.
   *
   * @param routingKey the routing key for the queue to send the message to (usually the queue name).
   * @param message the message to send.
   * @throws IOException if sending the message fails.
   */
  public void send(String routingKey, Message message) throws IOException {
    rabbitClient.send(routingConfig.getExchange(), routingKey, message);
  }

  /**
   * Sends multiple messages to a certain queue as JSON documents. The messages are sent in the same order
   * as returned by the iterator over <code>messages</code>.
   *
   * @param routingKey the routing key for the queue to send the message to (usually the queue name).
   * @param messages the messages to send.
   * @throws IOException if sending a message fails.
   */
  public void send(String routingKey, Collection<Message> messages) throws IOException {
    for (Message message : messages) {
      send(routingKey, message);
    }
  }

  private void retry(Message message) throws IOException {
    LOGGER.debug("Send message to failed queue: " + message);
    rabbitClient.send(routingConfig.getDeadLetterExchange(), routingConfig.getReadFrom(), message);
  }

  /**
   * Gets one message from the queue but does not acknowledge it. To do so, use {@link MessageBroker#ack(Message)}.
   *
   * @param queueName the queue to receive.
   * @return the received message.
   * @throws IOException if communication with RabbitMQ failed.
   */
  public Message receive(String queueName) throws IOException {
    return rabbitClient.receive(queueName);
  }

  /**
   * Gets one message from the input queue but does not acknowledge it. To do so, use {@link MessageBroker#ack(Message)}.
   *
   * @return the received message.
   * @throws IOException if communication with RabbitMQ failed.
   */
  public Message receive() throws IOException {
    return receive(routingConfig.getReadFrom());
  }

  private void provideInputQueues() throws IOException {
    final String deadLetterExchange = routingConfig.getDeadLetterExchange();
    final String exchange = routingConfig.getExchange();
    final String failedQueue = routingConfig.getFailedQueue();
    final String readFrom = routingConfig.getReadFrom();
    final String retryQueue = routingConfig.getRetryQueue();

    rabbitClient.declareQueue(readFrom, exchange, readFrom, null);
    rabbitClient.declareQueue(retryQueue, deadLetterExchange, readFrom,
        Maps.of(
            MESSAGE_TTL, config.getDeadLetterWait(),
            DEAD_LETTER_EXCHANGE, exchange)
    );
    rabbitClient.declareQueue(failedQueue, exchange, failedQueue, null);
  }

  private void provideOutputQueue() throws IOException {
    rabbitClient.declareQueue(
        routingConfig.getWriteTo(),
        routingConfig.getExchange(),
        routingConfig.getWriteTo(),
        Maps.of(DEAD_LETTER_EXCHANGE, routingConfig.getDeadLetterExchange())
    );
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
   * Rejects a messaging and takes care of proper dead lettering, retries and, if the message failed too ofen, routing to the failed queue.
   *
   * @param message the message to reject
   * @throws IOException if communication with RabbitMQ failed
   */
  public void reject(Message message) throws IOException {
    final Meta meta = message.getMeta();
    ack(message);
    if (meta.getRetries() < config.getMaxRetries()) {
      meta.setRetries(meta.getRetries() + 1);
      retry(message);
    } else if (routingConfig.hasFailedQueue()) {
      fail(message);
    }
  }

  private void fail(Message message) throws IOException {
    if (!routingConfig.hasFailedQueue()) {
      return;
    }
    LOGGER.debug("Send message to failed queue: " + message);
    send(routingConfig.getFailedQueue(), message);
  }

  private void provideExchanges() throws IOException {
    rabbitClient.provideExchange(routingConfig.getExchange());
    rabbitClient.provideExchange(routingConfig.getDeadLetterExchange());
  }

  public MessageBrokerConfig getConfig() {
    return config;
  }

  /**
   * Sends numbered test messages to the input queue.
   * @param n the number of messages to send
   * @throws IOException if sending a messages fails
   */
  public void createTestMessages(int n) throws IOException {
    for (int i = 0; i < n; i++) {
      String message = String.format("Test message #%d of %d", i, n);
      send(routingConfig.getReadFrom(), DefaultMessage.withType(message));
    }
  }

}
