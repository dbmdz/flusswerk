package de.digitalcollections.workflow.engine.messagebroker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import de.digitalcollections.workflow.engine.jackson.DefaultMessageMixin;
import de.digitalcollections.workflow.engine.jackson.EnvelopeMixin;
import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Envelope;
import de.digitalcollections.workflow.engine.model.Message;
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

  public RabbitClient(MessageBrokerConfig config, MessageBrokerConnection connection) {
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
    channel.basicPublish(exchange, routingKey, properties, data);
  }

  Message deserialize(String body) throws IOException {
    return objectMapper.readValue(body, messageClass);
  }

  byte[] serialize(Message message) throws IOException {
    return objectMapper.writeValueAsBytes(message);
  }

  public void ack(Message message) throws IOException {
    channel.basicAck(message.getEnvelope().getDeliveryTag(), SINGLE_MESSAGE);
  }

  public Message receive(String queueName) throws IOException {
    GetResponse response = channel.basicGet(queueName, NO_AUTO_ACK);
    if (response != null) {
      String body = new String(response.getBody(), StandardCharsets.UTF_8);
      Message message = deserialize(body);
      message.getEnvelope().setBody(body);
      message.getEnvelope().setDeliveryTag(response.getEnvelope().getDeliveryTag());
      return message;
    }
    return null;
  }

  public void provideExchange(String exchange) throws IOException {
    channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, DURABLE);
  }

  public void declareQueue(String name, String exchange, String routingKey, Map<String, Object> args) throws IOException {
    channel.queueDeclare(name, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, args);
    channel.queueBind(name, exchange, routingKey);
  }

}
