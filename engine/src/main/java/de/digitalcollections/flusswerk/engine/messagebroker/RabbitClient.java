package de.digitalcollections.flusswerk.engine.messagebroker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import de.digitalcollections.flusswerk.engine.jackson.DefaultMessageMixin;
import de.digitalcollections.flusswerk.engine.jackson.EnvelopeMixin;
import de.digitalcollections.flusswerk.engine.model.DefaultMessage;
import de.digitalcollections.flusswerk.engine.model.Envelope;
import de.digitalcollections.flusswerk.engine.model.Message;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

class RabbitClient {

  private static final boolean DURABLE = true;

  private static final boolean NO_AUTO_DELETE = false;

  private static final boolean NO_AUTO_ACK = false;

  private static final boolean NOT_EXCLUSIVE = false;

  private static final Integer PERSISTENT = 2;

  private static final boolean SINGLE_MESSAGE = false;

  private final Channel channel;

  private final ObjectMapper objectMapper;

  private Class<? extends Message> messageClass;

  private RabbitConnection connection;

  public RabbitClient(MessageBrokerConfig config, RabbitConnection connection) {
    this.connection = connection;
    channel = connection.getChannel();
    objectMapper = new ObjectMapper();
    messageClass = config.getMessageClass();
    objectMapper.registerModules(config.getJacksonModules());
    objectMapper.addMixIn(Envelope.class, EnvelopeMixin.class);
    objectMapper.registerModule(new JavaTimeModule());
    if (objectMapper.findMixInClassFor(Message.class) == null) {
      objectMapper.addMixIn(DefaultMessage.class, DefaultMessageMixin.class);
    }
  }

  void send(String exchange, String routingKey, Message message) throws IOException {
    byte[] data = serialize(message);
    AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
        .contentType("application/json")
        .deliveryMode(PERSISTENT)
        .build();

    try {
      channel.basicPublish(exchange, routingKey, properties, data);
    } catch (IOException e) {
      waitForConnection("Could not publish message to " + routingKey);
      channel.basicPublish(exchange, routingKey, properties, data);
    }
  }

  Message deserialize(String body) throws IOException {
    return objectMapper.readValue(body, messageClass);
  }

  byte[] serialize(Message message) throws IOException {
    return objectMapper.writeValueAsBytes(message);
  }

  public void ack(Message message) throws IOException {
    try {
      channel.basicAck(message.getEnvelope().getDeliveryTag(), SINGLE_MESSAGE);
    } catch (IOException e) {
      waitForConnection("Could not ack message");
      channel.basicAck(message.getEnvelope().getDeliveryTag(), SINGLE_MESSAGE);
    }
  }

  private void waitForConnection(String errorMessage) throws IOException {
    try {
      connection.waitForConnection();
    } catch (IOException e) {
      throw new IOException(errorMessage, e);
    }
  }

  public Message receive(String queueName) throws IOException {
    GetResponse response;
    try {
      response = channel.basicGet(queueName, NO_AUTO_ACK);
    } catch (IOException e) {
      waitForConnection("Could not receive message from " + queueName);
      response = channel.basicGet(queueName, NO_AUTO_ACK);
    }
    if (response != null) {
      String body = new String(response.getBody(), StandardCharsets.UTF_8);
      Message message = deserialize(body);
      message.getEnvelope().setBody(body);
      message.getEnvelope().setDeliveryTag(response.getEnvelope().getDeliveryTag());
      message.getEnvelope().setSource(queueName);
      return message;
    }
    return null;
  }

  public void provideExchange(String exchange) throws IOException {
    GetResponse response;
    try {
      channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, DURABLE);
    } catch (IOException e) {
      waitForConnection("Could not declare exchange");
      channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, DURABLE);
    }
  }


  public void declareQueue(String name, String exchange, String routingKey, Map<String, Object> args) throws IOException {
    createQueue(name, args);
    bindQueue(name, exchange, routingKey);
  }

  public void createQueue(String name, Map<String, Object> args) throws IOException {
    try {
      channel.queueDeclare(name, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, args);
    } catch (IOException e) {
      waitForConnection("Could not declare queue");
      channel.queueDeclare(name, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, args);
    }
  }

  public void bindQueue(String name, String exchange, String routingKey) throws IOException {
    try {
      channel.queueBind(name, exchange, routingKey);
    } catch (IOException e) {
      waitForConnection("Could not bind queue to exchange");
      channel.queueBind(name, exchange, routingKey);
    }
  }

  public Long getMessageCount(String queue) throws IOException {
    return channel.messageCount(queue);
  }

  public boolean isConnectionOk() {
    return connection.isOk();
  }

  public boolean isChannelAvailable() {
    return channel.isOpen();
  }

}
