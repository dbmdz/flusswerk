package de.digitalcollections.workflow.engine;

import de.digitalcollections.workflow.engine.model.Message;
import de.digitalcollections.workflow.engine.model.Meta;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
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

  MessageBroker(MessageBrokerConfig config, MessageBrokerConnection connection) throws IOException {
    deadLetterWait = config.getDeadLetterWait();
    maxRetries = config.getMaxRetries();
    routingConfig = config.getRoutingConfig();

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

  void provideInputQueues() throws IOException {
    rabbitClient.declareQueue(
        routingConfig.getReadFrom(),
        routingConfig.getExchange(),
        routingConfig.getReadFrom(),
        null
    );

    rabbitClient.declareQueue(
        routingConfig.getRetryQueue(),
        routingConfig.getDeadLetterExchange(),
        routingConfig.getReadFrom(),
        Map.of(
            MESSAGE_TTL, deadLetterWait,
            DEAD_LETTER_EXCHANGE, routingConfig.getExchange())
    );

    rabbitClient.declareQueue(routingConfig.getFailedQueue(),
        routingConfig.getExchange(),
        routingConfig.getFailedQueue(),
        null
    );
  }

  private void provideOutputQueue() throws IOException {
    rabbitClient.declareQueue(
        routingConfig.getWriteTo(),
        routingConfig.getExchange(),
        routingConfig.getWriteTo(),
        Map.of(DEAD_LETTER_EXCHANGE, routingConfig.getDeadLetterExchange())
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
