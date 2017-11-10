package org.mdz.dzp.workflow.neo.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.mdz.dzp.workflow.neo.engine.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

public class RabbitMQ {

  private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQ.class);

  private static final Integer PERSISTENT = 2;
  private static final boolean DURABLE = true;
  private static final boolean NOT_EXCLUSIVE = false;
  private static final boolean NO_AUTO_DELETE = false;
  private static final boolean NO_AUTO_ACK = false;
  private static final boolean SINGLE_MESSAGE = false;
  private static final boolean DONT_REQUEUE = false;

  private final String username = "guest";

  private final String password = "guest";

  private final String virtualHost = "/";

  private final String hostName = "localhost";

  private final int port = 5672;

  private final ObjectMapper objectMapper;

  private final Connection conn;

  private final Channel channel;

  private final String dlxName = "testDlx";

  private final String exchange = "testExchange";

  public RabbitMQ() throws IOException, TimeoutException {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setUsername(username);
    factory.setPassword(password);
    factory.setVirtualHost(virtualHost);
    factory.setHost(hostName);
    factory.setPort(port);

    conn = factory.newConnection();
    channel = conn.createChannel();

    channel.exchangeDeclare("testExchange", "direct", true);
    channel.exchangeDeclare("testDlx", "direct", true);

    objectMapper = new ObjectMapper();
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
      message.setBody(new String(response.getBody(), "UTF-8"));
      message.setDeliveryTag(response.getEnvelope().getDeliveryTag());
      return message;
    }
    return null;
  }

  public void provideInputQueue(String inputChannel) throws IOException {
    requireNonNull(inputChannel);
    Map<String, Object> queueArgs = new HashMap<>();
    queueArgs.put("x-dead-letter-exchange", dlxName);
    channel.queueDeclare(inputChannel, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, queueArgs);
    channel.queueBind(inputChannel, exchange, inputChannel);

    Map<String, Object> dlxQueueArgs = new HashMap<>();
    dlxQueueArgs.put("x-message-ttl", 1000 * 30);
    dlxQueueArgs.put("x-dead-letter-exchange", exchange);
    channel.queueDeclare("dlx."+inputChannel, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, dlxQueueArgs);
    channel.queueBind("dlx." + inputChannel, dlxName, inputChannel);

    channel.queueDeclare("failed." + inputChannel, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, null);
    channel.queueBind("failed." + inputChannel, dlxName, inputChannel);
  }

  public void provideOutputQueue(String queueName) throws IOException {
    requireNonNull(queueName);
    Map<String, Object> queueArgs = new HashMap<>();
    queueArgs.put("x-dead-letter-exchange", dlxName);
    channel.queueDeclare(queueName, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, queueArgs);
    channel.queueBind(queueName, exchange, queueName);
  }

  public void ack(Message message) throws IOException {
    channel.basicAck(message.getDeliveryTag(), SINGLE_MESSAGE);
  }

  public void reject(Message message) throws IOException {
    channel.basicReject(message.getDeliveryTag(), DONT_REQUEUE);
  }
}
