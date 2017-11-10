package org.mdz.dzp.workflow.neo.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.nio.charset.Charset;
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
  public static final boolean DURABLE = true;
  public static final boolean NOT_EXCLUSIVE = false;
  public static final boolean NO_AUTO_DELETE = false;

  private final String username = "guest";

  private final String password = "guest";

  private final String virtualHost = "/";

  private final String hostName = "localhost";

  private final int port = 5672;

  private final ObjectMapper objectMapper;

  private final Connection conn;

  private final Channel channel;

  private final String dlxName = "testDlx";
  private final String exchange = "testExchange";;


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

//    Map<String, Object> queueArgs = new HashMap<>();
//    queueArgs.put("x-dead-letter-exchange", "testDlx");
//    channel.queueDeclare("testQueue", true, false, false, queueArgs);
//    channel.queueBind("testQueue", "testExchange", "testQueue");
//
//    Map<String, Object> dlxQueueArgs = new HashMap<>();
//    dlxQueueArgs.put("x-message-ttl", 1000 * 30);
//    dlxQueueArgs.put("x-dead-letter-exchange", "testExchange");
//    channel.queueDeclare("dlx.testQueue", true, false, false, dlxQueueArgs);
//    channel.queueBind("dlx.testQueue", "testDlx", "testQueue");

    channel.queueDeclare("failed.testQueue", true, false, false, null);
    channel.queueBind("failed.testQueue", "testDlx", "failedQueue");

    objectMapper = new ObjectMapper();
  }

  public void send(Message message) throws IOException {
    byte[] data = objectMapper.writeValueAsBytes(message);
    AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
        .contentType("application/json")
        .deliveryMode(PERSISTENT)
        .build();
    channel.basicPublish("testExchange", "testQueue", properties, data);
  }

  public void registerConsumer(String queue, boolean autoAck, String consumerTag, MessageListener callback) throws IOException {
    while (true) {
      LOGGER.info("Start new consumer");
      channel.basicConsume(queue, autoAck, consumerTag, new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {
          LOGGER.info("Got message: " + new String(body, Charset.defaultCharset()));
          if (!"application/json".equals(properties.getContentType())) {
            LOGGER.error("Cannot process message " + new String(body, Charset.defaultCharset()) + " because its content type is not application/json");
            channel.basicReject(envelope.getDeliveryTag(), false);
            return;
          }

          LOGGER.info(String.format("routingKey = %s", envelope.getRoutingKey()));
          LOGGER.info(String.format("contentType = %s", properties.getContentType()));
          LOGGER.info(String.format("deliveryTag = %d", envelope.getDeliveryTag()));
          LOGGER.info(String.format("body = %s", new String(body, Charset.defaultCharset())));

          Message message = objectMapper.readValue(body, Message.class);
          callback.receive(message, envelope.getDeliveryTag(), channel);
        }
      });
    }
  }

  public Channel getChannel() {
    return channel;
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

}
