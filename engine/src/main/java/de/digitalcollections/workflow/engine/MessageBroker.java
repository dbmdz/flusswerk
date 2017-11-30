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
    provideInputQueue();
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

  public void sendToDlx(String routingKey, Message message) throws IOException {
    send(routingConfig.getDeadLetterExchange(), routingKey, message);
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

  void provideInputQueue() throws IOException {
    String exchange = routingConfig.getExchange();
    String deadLetterExchange = routingConfig.getDeadLetterExchange();

    Map<String, Object> queueArgs = new HashMap<>();
    String inputQueue = routingConfig.getReadFrom();
    channel.queueDeclare(inputQueue, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, queueArgs);
    channel.queueBind(inputQueue, exchange, inputQueue);

    Map<String, Object> dlxQueueArgs = new HashMap<>();
    dlxQueueArgs.put("x-message-ttl", deadLetterWait);
    dlxQueueArgs.put("x-dead-letter-exchange", exchange);
    String dlxQueue = routingConfig.getRetryQueue();
    channel.queueDeclare(dlxQueue, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, dlxQueueArgs);
    channel.queueBind(dlxQueue, deadLetterExchange, inputQueue);

    String failedQueue = routingConfig.getFailedQueue();
    channel.queueDeclare(failedQueue, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, null);
    channel.queueBind(failedQueue, exchange, failedQueue);
  }

  private void provideOutputQueue() throws IOException {
    String outputQueue = routingConfig.getWriteTo();
    Map<String, Object> queueArgs = new HashMap<>();
    queueArgs.put("x-dead-letter-exchange", routingConfig.getDeadLetterExchange());
    channel.queueDeclare(outputQueue, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, queueArgs);
    channel.queueBind(outputQueue, routingConfig.getExchange(), outputQueue);
  }

  public void ack(Message message) throws IOException {
    channel.basicAck(message.getMeta().getDeliveryTag(), SINGLE_MESSAGE);
  }

  public void reject(Message message) throws IOException {
    final Meta meta = message.getMeta();
    ack(message);
    if (meta.getRetries() < maxRetries) {
      meta.setRetries(meta.getRetries() + 1);
      LOGGER.debug("Send message to DLX: " + message);
      sendToDlx(routingConfig.getReadFrom(), message);
    } else {
      if (routingConfig.hasFailedQueue()) {
        LOGGER.debug("Send message to failed inputQueue: " + message);
        send(routingConfig.getFailedQueue(), message);
      }
    }
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
