package de.digitalcollections.workflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import de.digitalcollections.workflow.engine.jackson.MetaMixin;
import de.digitalcollections.workflow.engine.model.Message;
import de.digitalcollections.workflow.engine.model.Meta;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageBroker {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageBroker.class);

  private static final Integer PERSISTENT = 2;
  private static final boolean DURABLE = true;
  private static final boolean NOT_EXCLUSIVE = false;
  private static final boolean NO_AUTO_DELETE = false;
  private static final boolean NO_AUTO_ACK = false;
  private static final boolean SINGLE_MESSAGE = false;
  private static final boolean DO_NOT_REQUEUE = false;

  private final ObjectMapper objectMapper;

  private final Channel channel;

  private final int deadLetterWait;

  private int maxRetries;

  private Class<? extends Message> messageClass;

  private RoutingConfig routingConfig;

  MessageBroker(MessageBrokerConfig config, MessageBrokerConnection connection) throws IOException {
    channel = connection.getChannel();
    objectMapper = config.getObjectMapper();
    messageClass = config.getMessageClass();
    if (objectMapper.findMixInClassFor(Message.class) == null) {
      objectMapper.addMixIn(messageClass, config.getMessageMixin());
    }
    objectMapper.addMixIn(Meta.class, MetaMixin.class);
    objectMapper.registerModule(new JavaTimeModule());
    deadLetterWait = config.getDeadLetterWait();
    maxRetries = config.getMaxRetries();

    this.routingConfig = config.getRoutingConfig();
    provideExchanges(routingConfig.getExchange(), routingConfig.getDeadLetterExchange());
    provideInputQueues();
    if (routingConfig.hasWriteTo()) {
      provideOutputQueue();
    }
  }

  private void send(String exchange, String routingKey, Message message) throws IOException {
    byte[] data = serialize(message);
    AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
        .contentType("application/json")
        .deliveryMode(PERSISTENT)
        .build();
    channel.basicPublish(exchange, routingKey, properties, data);
  }

  public void send(Message message) throws IOException {
    send(routingConfig.getExchange(), routingConfig.getWriteTo(), message);
  }

  public void send(String routingKey, Message message) throws IOException {
    send(routingConfig.getExchange(), routingKey, message);
  }

  public void send(String routingKey, Collection<Message> messages) throws IOException {
    for (Message message : messages) {
      send(routingKey, message);
    }
  }

  public void retry(Message message) throws IOException {
    LOGGER.debug("Send message to failed queue: " + message);
    send(routingConfig.getDeadLetterExchange(), routingConfig.getReadFrom(), message);
  }

  public Message receive(String queueName) throws IOException {
    GetResponse response = channel.basicGet(queueName, NO_AUTO_ACK);
    if (response != null) {
      String body = new String(response.getBody(), StandardCharsets.UTF_8);
      Message message = deserialize(body);
      message.getMeta().setBody(body);
      message.getMeta().setDeliveryTag(response.getEnvelope().getDeliveryTag());
      return message;
    }
    return null;
  }

  public Message receive() throws IOException {
    return receive(routingConfig.getReadFrom());
  }

  Message deserialize(String body) throws IOException {
    return objectMapper.readValue(body, messageClass);
  }

  byte[] serialize(Message message) throws IOException {
    return objectMapper.writeValueAsBytes(message);
  }

  void provideInputQueues() throws IOException {
    declareQueue(
        routingConfig.getReadFrom(),
        routingConfig.getExchange(),
        routingConfig.getReadFrom(),
        null
    );

    Map<String, Object> dlxQueueArgs = new HashMap<>();
    dlxQueueArgs.put("x-message-ttl", deadLetterWait);
    dlxQueueArgs.put("x-dead-letter-exchange", routingConfig.getExchange());
    declareQueue(
        routingConfig.getRetryQueue(),
        routingConfig.getDeadLetterExchange(),
        routingConfig.getReadFrom(),
        dlxQueueArgs
    );

    declareQueue(routingConfig.getFailedQueue(),
        routingConfig.getExchange(),
        routingConfig.getFailedQueue(),
        null
    );
  }

  private void declareQueue(String name, String exchange, String routingKey, Map<String, Object> args) throws IOException {
    channel.queueDeclare(name, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, args);
    channel.queueBind(name, exchange, routingKey);
  }

  private void provideOutputQueue() throws IOException {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", routingConfig.getDeadLetterExchange());
    declareQueue(
        routingConfig.getWriteTo(),
        routingConfig.getExchange(),
        routingConfig.getWriteTo(),
        args
    );
  }

  public void ack(Message message) throws IOException {
    channel.basicAck(message.getMeta().getDeliveryTag(), SINGLE_MESSAGE);
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

  public void provideExchanges(String exchange, String deadLetterExchange) throws IOException {
    channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, DURABLE);
    channel.exchangeDeclare(deadLetterExchange, BuiltinExchangeType.TOPIC, DURABLE);
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
