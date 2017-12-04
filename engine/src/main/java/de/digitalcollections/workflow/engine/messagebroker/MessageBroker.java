package de.digitalcollections.workflow.engine.messagebroker;

import de.digitalcollections.workflow.engine.model.Message;
import de.digitalcollections.workflow.engine.model.Meta;
import de.digitalcollections.workflow.engine.util.Maps;
import java.io.IOException;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageBroker {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageBroker.class);
  private static final String MESSAGE_TTL = "x-message-ttl";
  private static final String DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";

  private final int deadLetterWait;

  private int maxRetries;

  private RoutingConfig routingConfig;

  private final RabbitClient rabbitClient;

  MessageBroker(MessageBrokerConfig config, MessageBrokerConnection connection, RoutingConfig routingConfig) throws IOException {
    deadLetterWait = config.getDeadLetterWait();
    maxRetries = config.getMaxRetries();
    this.routingConfig = routingConfig;

    rabbitClient = new RabbitClient(config, connection);

    provideExchanges();
    provideInputQueues();
    if (routingConfig.hasWriteTo()) {
      provideOutputQueue();
    }
  }

  public void send(Message message) throws IOException {
    rabbitClient.send(routingConfig.getExchange(), routingConfig.getWriteTo(), message);
  }

  public void send(String routingKey, Message message) throws IOException {
    rabbitClient.send(routingConfig.getExchange(), routingKey, message);
  }

  public void send(String routingKey, Collection<Message> messages) throws IOException {
    for (Message message : messages) {
      send(routingKey, message);
    }
  }

  private void retry(Message message) throws IOException {
    LOGGER.debug("Send message to failed queue: " + message);
    rabbitClient.send(routingConfig.getDeadLetterExchange(), routingConfig.getReadFrom(), message);
  }

  public Message receive(String queueName) throws IOException {
    return rabbitClient.receive(queueName);
  }

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
            MESSAGE_TTL, deadLetterWait,
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

  public void ack(Message message) throws IOException {
    rabbitClient.ack(message);
  }

  public void reject(Message message) throws IOException {
    final Meta meta = message.getMeta();
    ack(message);
    if (meta.getRetries() < maxRetries) {
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

  public int getDeadLetterWait() {
    return deadLetterWait;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public RoutingConfig getRoutingConfig() {
    return routingConfig;
  }
}
