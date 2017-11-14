package org.mdz.dzp.workflow.neo.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.mdz.dzp.workflow.neo.engine.jackson.MessageModule;
import org.mdz.dzp.workflow.neo.engine.model.Message;
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

  private final String deadLetterExchange = "testDlx";

  private final String exchange = "testExchange";

  private String failedQueue;

  private int maxRetries;

  MessageBroker(MessageBrokerConfig config, MessageBrokerConnection connection) throws IOException, TimeoutException {
    channel = connection.getChannel();
    objectMapper = config.getObjectMapper();
    objectMapper.registerModule(new MessageModule());
    deadLetterWait = config.getDeadLetterWait();
    maxRetries = config.getMaxRetries();
  }

  public void send(String routingKey, Message message) throws IOException {
    byte[] data = objectMapper.writeValueAsBytes(message);
    AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
        .contentType("application/json")
        .deliveryMode(PERSISTENT)
        .build();
    channel.basicPublish(exchange, routingKey, properties, data);
  }

  public Message receive(String queueName) throws IOException {
    GetResponse response = channel.basicGet(queueName, NO_AUTO_ACK);
    if (response != null) {
      Message message = objectMapper.readValue(response.getBody(), Message.class);
      message.setBody(new String(response.getBody(), StandardCharsets.UTF_8));
      message.setDeliveryTag(response.getEnvelope().getDeliveryTag());
      return message;
    }
    return null;
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
    channel.basicAck(message.getDeliveryTag(), SINGLE_MESSAGE);
  }

  public void reject(Message message) throws IOException {
    if (message.getRetries() < maxRetries) {
      message.setRetries(message.getRetries() + 1);
      channel.basicReject(message.getDeliveryTag(), DO_NOT_REQUEUE);
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
