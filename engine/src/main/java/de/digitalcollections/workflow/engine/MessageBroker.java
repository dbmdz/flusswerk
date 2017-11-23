package de.digitalcollections.workflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import de.digitalcollections.workflow.engine.jackson.MetaMixin;
import de.digitalcollections.workflow.engine.model.Message;
import de.digitalcollections.workflow.engine.model.Meta;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

public class MessageBroker {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageBroker.class);

  private static final Integer PERSISTENT = 2;
  private static final boolean DURABLE = true;
  private static final boolean NOT_EXCLUSIVE = false;
  private static final boolean NO_AUTO_DELETE = false;
  private static final boolean NO_AUTO_ACK = false;
  private static final boolean SINGLE_MESSAGE = false;
  private static final boolean DO_NOT_REQUEUE = false;
  private static final String DIRECT = "direct";

  private final ObjectMapper objectMapper;

  private final Channel channel;

  private final int deadLetterWait;

  private String deadLetterExchange;

  private String exchange;

  private String failedQueue;

  private int maxRetries;

  private Class<? extends Message> messageClass;

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
    exchange = config.getExchange();
    deadLetterExchange = config.getDeadLetterExchange();
    provideExchanges(exchange, deadLetterExchange);
  }

  public void send(String routingKey, Message message) throws IOException {
    byte[] data = serialize(message);
    AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
        .contentType("application/json")
        .deliveryMode(PERSISTENT)
        .build();
    channel.basicPublish(exchange, routingKey, properties, data);
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

  Message deserialize(String body) throws IOException {
    return objectMapper.readValue(body, messageClass);
  }

  byte[] serialize(Message message) throws IOException {
    return objectMapper.writeValueAsBytes(message);
  }

  public void provideInputQueue(String queue) throws IOException {
    requireNonNull(queue);
    Map<String, Object> queueArgs = new HashMap<>();
    queueArgs.put("x-dead-letter-exchange", deadLetterExchange);
    channel.queueDeclare(queue, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, queueArgs);
    channel.queueBind(queue, exchange, queue);

    Map<String, Object> dlxQueueArgs = new HashMap<>();
    dlxQueueArgs.put("x-message-ttl", deadLetterWait);
    dlxQueueArgs.put("x-dead-letter-exchange", exchange);
    String dlxQueue = "dlx." + queue;
    channel.queueDeclare(dlxQueue, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, dlxQueueArgs);
    channel.queueBind(dlxQueue, deadLetterExchange, queue);

    failedQueue = "failed." + queue;
    channel.queueDeclare(failedQueue, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, null);
    channel.queueBind(failedQueue, deadLetterExchange, queue);
  }

  public void provideOutputQueue(String queue) throws IOException {
    requireNonNull(queue);
    Map<String, Object> queueArgs = new HashMap<>();
    queueArgs.put("x-dead-letter-exchange", deadLetterExchange);
    channel.queueDeclare(queue, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, queueArgs);
    channel.queueBind(queue, exchange, queue);
  }

  public void ack(Message message) throws IOException {
    channel.basicAck(message.getMeta().getDeliveryTag(), SINGLE_MESSAGE);
  }

  public void reject(Message message) throws IOException {
    final Meta meta = message.getMeta();
    if (meta.getRetries() < maxRetries) {
      meta.setRetries(meta.getRetries() + 1);
      channel.basicReject(meta.getDeliveryTag(), DO_NOT_REQUEUE);
    } else {
      ack(message);
      if (failedQueue != null) {
        send(failedQueue, message);
      }
    }
  }

  public void provideExchanges(String exchange, String deadLetterExchange) throws IOException {
    channel.exchangeDeclare(exchange, DIRECT, DURABLE);
    channel.exchangeDeclare(deadLetterExchange, DIRECT, DURABLE);
  }
}
