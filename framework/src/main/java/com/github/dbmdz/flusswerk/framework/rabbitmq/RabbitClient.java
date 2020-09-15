package com.github.dbmdz.flusswerk.framework.rabbitmq;

import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.jackson.FlusswerkObjectMapper;
import com.github.dbmdz.flusswerk.framework.model.Envelope;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RabbitClient {

  private static final boolean DURABLE = true;

  private static final boolean NO_AUTO_DELETE = false;

  private static final boolean NO_AUTO_ACK = false;

  private static final boolean NOT_EXCLUSIVE = false;

  private static final Integer PERSISTENT = 2;

  private static final boolean SINGLE_MESSAGE = false;

  private Channel channel;

  private final FlusswerkObjectMapper objectMapper;

  private final RabbitConnection connection;

  public RabbitClient(RabbitConnection rabbitConnection) {
    this(new IncomingMessageType(), rabbitConnection);
  }

  public RabbitClient(FlusswerkObjectMapper flusswerkObjectMapper, RabbitConnection connection) {
    this.connection = connection;
    channel = connection.getChannel();
    objectMapper = flusswerkObjectMapper;
  }

  public RabbitClient(IncomingMessageType incomingMessageType, RabbitConnection connection) {
    this(new FlusswerkObjectMapper(incomingMessageType), connection);
  }

  void send(String exchange, String routingKey, Message message) throws IOException {
    byte[] data = serialize(message);
    sendRaw(exchange, routingKey, data);
  }

  void sendRaw(String exchange, String routingKey, byte[] data) throws IOException {
    AMQP.BasicProperties properties =
        new AMQP.BasicProperties.Builder()
            .contentType("application/json")
            .deliveryMode(PERSISTENT)
            .build();

    try {
      channel.basicPublish(exchange, routingKey, properties, data);
    } catch (Exception e) {
      tryToReconnect("Could not publish message to " + routingKey);
      channel.basicPublish(exchange, routingKey, properties, data);
    }
  }

  Message deserialize(String body) throws IOException {
    return objectMapper.deserialize(body);
  }

  byte[] serialize(Message message) throws IOException {
    return objectMapper.writeValueAsBytes(message);
  }

  public void ack(Envelope envelope) throws IOException {
    try {
      channel.basicAck(envelope.getDeliveryTag(), SINGLE_MESSAGE);
    } catch (Exception e) {
      tryToReconnect("Could not ack message");
      channel.basicAck(envelope.getDeliveryTag(), SINGLE_MESSAGE);
    }
  }

  private void tryToReconnect(String errorMessage) throws IOException {
    try {
      connection.waitForConnection();
      channel = connection.getChannel();
    } catch (IOException e) {
      throw new IOException(errorMessage, e);
    }
  }

  public Message receive(String queueName) throws IOException, InvalidMessageException {
    GetResponse response;
    try {
      response = channel.basicGet(queueName, NO_AUTO_ACK);
    } catch (Exception e) {
      tryToReconnect("Could not receive message from " + queueName);
      response = channel.basicGet(queueName, NO_AUTO_ACK);
    }
    if (response != null) {
      String body = new String(response.getBody(), StandardCharsets.UTF_8);

      try {
        Message message = deserialize(body);
        message.getEnvelope().setBody(body);
        message.getEnvelope().setDeliveryTag(response.getEnvelope().getDeliveryTag());
        message.getEnvelope().setSource(queueName);
        return message;
      } catch (Exception e) {
        Envelope envelope = new Envelope();
        envelope.setBody(body);
        envelope.setDeliveryTag(response.getEnvelope().getDeliveryTag());
        envelope.setSource(queueName);
        throw new InvalidMessageException(envelope, e.getMessage(), e);
      }
    }
    return null;
  }

  public void provideExchange(String exchange) throws IOException {
    try {
      channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, DURABLE);
    } catch (Exception e) {
      tryToReconnect("Could not declare exchange");
      channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, DURABLE);
    }
  }

  public void declareQueue(
      String name, String exchange, String routingKey, Map<String, Object> args)
      throws IOException {
    createQueue(name, args);
    bindQueue(name, exchange, routingKey);
  }

  public void createQueue(String name, Map<String, Object> args) throws IOException {
    try {
      channel.queueDeclare(name, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, args);
    } catch (Exception e) {
      tryToReconnect("Could not declare queue");
      channel.queueDeclare(name, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, args);
    }
  }

  public void bindQueue(String name, String exchange, String routingKey) throws IOException {
    try {
      channel.queueBind(name, exchange, routingKey);
    } catch (Exception e) {
      tryToReconnect("Could not bind queue to exchange");
      channel.queueBind(name, exchange, routingKey);
    }
  }

  public Long getMessageCount(String queue) throws IOException {
    return channel.messageCount(queue);
  }

  public boolean isChannelAvailable() {
    return channel.isOpen();
  }

  Channel getChannel() {
    return channel;
  }
}
