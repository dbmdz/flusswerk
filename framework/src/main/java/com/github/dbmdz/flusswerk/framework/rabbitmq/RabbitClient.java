package com.github.dbmdz.flusswerk.framework.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.jackson.FlusswerkObjectMapper;
import com.github.dbmdz.flusswerk.framework.model.Envelope;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoverableChannel;
import com.rabbitmq.client.RecoveryListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitClient {

  private static final boolean DURABLE = true;

  private static final boolean NO_AUTO_DELETE = false;

  private static final boolean NO_AUTO_ACK = false;

  private static final boolean NOT_EXCLUSIVE = false;

  private static final Integer PERSISTENT = 2;

  private static final boolean SINGLE_MESSAGE = false;

  private static final Logger log = LoggerFactory.getLogger(RabbitClient.class);

  private final Channel channel;

  private final Lock channelLock = new ReentrantLock();
  private final Condition channelAvailableAgain = channelLock.newCondition();
  private boolean channelAvailable = true;

  private final FlusswerkObjectMapper objectMapper;

  public RabbitClient(RabbitConnection rabbitConnection) {
    this(new IncomingMessageType(), rabbitConnection);
  }

  public RabbitClient(FlusswerkObjectMapper flusswerkObjectMapper, RabbitConnection connection) {
    channel = connection.getChannel();
    // We need a recoverable connection since we don't want to handle connection and channel
    // recovery ourselves.
    if (channel instanceof RecoverableChannel) {
      RecoverableChannel rc = (RecoverableChannel) channel;
      rc.addRecoveryListener(
          new RecoveryListener() {
            @Override
            public void handleRecovery(Recoverable recoverable) {
              // Whenever a connection has failed and is then automatically recovered, we want to
              // reset the availability flag and signal all threads that are currently waiting for
              // the connection's channel to become available again so they can retry their
              // respective channel operation.
              log.info("Connection recovered.");
              channelAvailable = true;
              channelLock.lock();
              channelAvailableAgain.signalAll();
              channelLock.unlock();
            }

            @Override
            public void handleRecoveryStarted(Recoverable recoverable) {
              // NOP
            }
          });
    } else {
      throw new RuntimeException("Flusswerk needs a recoverable connection to RabbitMQ");
    }
    objectMapper = flusswerkObjectMapper;
  }

  public RabbitClient(IncomingMessageType incomingMessageType, RabbitConnection connection) {
    this(new FlusswerkObjectMapper(incomingMessageType), connection);
  }

  void send(String exchange, String routingKey, Message message) throws IOException {
    byte[] data = serialize(message);
    sendRaw(exchange, routingKey, data);
  }

  void sendRaw(String exchange, String routingKey, byte[] data) {
    AMQP.BasicProperties properties =
        new AMQP.BasicProperties.Builder()
            .contentType("application/json")
            .deliveryMode(PERSISTENT)
            .build();

    // The channel might not be available or become unavailable due to a connection error. In this
    // case, we wait until the connection becomes available again.
    while (true) {
      if (channelAvailable) {
        try {
          channel.basicPublish(exchange, routingKey, properties, data);
          break;
        } catch (IOException | AlreadyClosedException e) {
          log.warn(
              "Failed to publish message to RabbitMQ: '{}', waiting for channel to become available again",
              e.getMessage());
          channelAvailable = false;
        }
      }
      // We loop here because the signal might be triggered due to what the JVM documentation calls
      // a 'spurious wakeup', i.e. the signal is triggered even though no connection recovery has
      // yet happened.
      while (!channelAvailable) {
        channelLock.lock();
        channelAvailableAgain.awaitUninterruptibly();
        channelLock.unlock();
      }
    }
  }

  Message deserialize(String body) throws JsonProcessingException {
    return objectMapper.deserialize(body);
  }

  byte[] serialize(Message message) throws IOException {
    return objectMapper.writeValueAsBytes(message);
  }

  public void ack(Envelope envelope) {
    // The channel might not be available or become unavailable due to a connection error. In this
    // case, we wait until the connection becomes available again.
    while (true) {
      if (channelAvailable) {
        try {
          channel.basicAck(envelope.getDeliveryTag(), SINGLE_MESSAGE);
          break;
        } catch (IOException | AlreadyClosedException e) {
          log.warn("Failed to ACK message to RabbitMQ: {}", e.getMessage(), e);
          channelAvailable = false;
        }
      }
      // We loop here because the signal might be triggered due to what the JVM documentation calls
      // a 'spurious wakeup', i.e. the signal is triggered even though no connection recovery has
      // yet happened.
      while (!channelAvailable) {
        channelLock.lock();
        channelAvailableAgain.awaitUninterruptibly();
        channelLock.unlock();
      }
    }
  }

  public Message receive(String queueName, boolean autoAck) throws InvalidMessageException {
    GetResponse response;
    // The channel might not be available or become unavailable due to a connection error. In this
    // case, we wait until the connection becomes available again.
    while (true) {
      if (channelAvailable) {
        try {
          response = channel.basicGet(queueName, autoAck);
          break;
        } catch (IOException | AlreadyClosedException e) {
          log.warn("Failed to receive message from RabbitMQ: {}", e.getMessage(), e);
          channelAvailable = false;
        }
      }
      // We loop here because the signal might be triggered due to what the JVM documentation calls
      // a 'spurious wakeup', i.e. the signal is triggered even though no connection recovery has
      // yet happened.
      while (!channelAvailable) {
        channelLock.lock();
        channelAvailableAgain.awaitUninterruptibly();
        channelLock.unlock();
      }
    }

    if (response == null) {
      return null;
    }

    String body = new String(response.getBody(), StandardCharsets.UTF_8);
    try {
      Message message = deserialize(body);
      message.getEnvelope().setBody(body);
      message.getEnvelope().setDeliveryTag(response.getEnvelope().getDeliveryTag());
      message.getEnvelope().setSource(queueName);
      return message;
    } catch (JsonProcessingException e) {
      Envelope envelope = new Envelope();
      envelope.setBody(body);
      envelope.setDeliveryTag(response.getEnvelope().getDeliveryTag());
      envelope.setSource(queueName);
      throw new InvalidMessageException(envelope, e.getMessage(), e);
    }
  }

  public void provideExchange(String exchange) throws IOException {
    channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, DURABLE);
  }

  public void declareQueue(
      String name, String exchange, String routingKey, Map<String, Object> args)
      throws IOException {
    createQueue(name, args);
    bindQueue(name, exchange, routingKey);
  }

  public void createQueue(String name, Map<String, Object> args) throws IOException {
    channel.queueDeclare(name, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, args);
  }

  public void bindQueue(String name, String exchange, String routingKey) throws IOException {
    channel.queueBind(name, exchange, routingKey);
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
