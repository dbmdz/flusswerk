package dev.mdz.flusswerk.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.*;
import dev.mdz.flusswerk.engine.FlusswerkConsumer;
import dev.mdz.flusswerk.exceptions.InvalidMessageException;
import dev.mdz.flusswerk.jackson.FlusswerkObjectMapper;
import dev.mdz.flusswerk.model.IncomingMessageType;
import dev.mdz.flusswerk.model.Message;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitClient {

  private static final boolean DURABLE = true;

  private static final boolean AUTO_DELETE = false;

  private static final boolean EXCLUSIVE = false;

  private static final Integer PERSISTENT = 2;

  private static final boolean MULTIPLE_MESSAGES = false;

  private static final Logger log = LoggerFactory.getLogger(RabbitClient.class);

  private final RabbitConnection connection;
  private final Channel channel;
  private final ChannelCommands commands;
  private final Lock channelLock = new ReentrantLock();
  private final Condition channelAvailableAgain = channelLock.newCondition();
  private final FlusswerkObjectMapper objectMapper;
  private boolean channelAvailable = true;
  private final List<ChannelListener> channelListeners = new ArrayList<>();

  public RabbitClient(RabbitConnection rabbitConnection) {
    this(new IncomingMessageType(), rabbitConnection);
  }

  public RabbitClient(FlusswerkObjectMapper flusswerkObjectMapper, RabbitConnection connection) {
    this.connection = connection;
    channel = connection.getChannel();
    // We need a recoverable connection since we don't want to handle connection and channel
    // recovery ourselves.
    if (channel instanceof RecoverableChannel rc) {
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
    commands = new ChannelCommands(channel);
    objectMapper = flusswerkObjectMapper;
  }

  public RabbitClient(IncomingMessageType incomingMessageType, RabbitConnection connection) {
    this(new FlusswerkObjectMapper(incomingMessageType), connection);
  }

  void send(String exchange, String routingKey, Message message) {
    byte[] data = serialize(message);
    sendRaw(exchange, routingKey, data);
  }

  void sendRaw(String exchange, String routingKey, byte[] data) {
    AMQP.BasicProperties properties =
        new AMQP.BasicProperties.Builder()
            .contentType("application/json")
            .deliveryMode(PERSISTENT)
            .build();

    execute(commands.basicPublish(exchange, routingKey, properties, data));
  }

  Message deserialize(String body) throws JsonProcessingException {
    return objectMapper.deserialize(body);
  }

  byte[] serialize(Message message) {
    try {
      return objectMapper.writeValueAsBytes(message);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Cannot serialize message", e);
    }
  }

  public void ack(dev.mdz.flusswerk.model.Envelope envelope) {
    ack(envelope.getDeliveryTag());
  }

  public void ack(long deliveryTag) {
    execute(commands.basicAck(deliveryTag, MULTIPLE_MESSAGES));
  }

  public void reject(Envelope envelope, boolean requeue) {
    execute(commands.basicReject(envelope.getDeliveryTag(), requeue));
  }

  public Message receive(String queueName, boolean autoAck) throws InvalidMessageException {
    GetResponse response = execute(commands.basicGet(queueName, autoAck));
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
      dev.mdz.flusswerk.model.Envelope envelope = new dev.mdz.flusswerk.model.Envelope();
      envelope.setBody(body);
      envelope.setDeliveryTag(response.getEnvelope().getDeliveryTag());
      envelope.setSource(queueName);
      throw new InvalidMessageException(envelope, e.getMessage(), e);
    }
  }

  public void consume(FlusswerkConsumer consumer, boolean autoAck) {
    execute(commands.basicConsume(consumer.getInputQueue(), autoAck, consumer));
  }

  public void nack(long deliveryTag, boolean multiple, boolean requeue) throws IOException {
    channel.basicNack(deliveryTag, multiple, requeue);
  }

  public void cancel(String consumerTag) throws IOException {
    channel.basicCancel(consumerTag);
  }

  public void provideExchange(String exchange) {
    execute(commands.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, DURABLE));
  }

  public void declareQueue(
      String name, String exchange, String routingKey, Map<String, Object> args) {
    createQueue(name, args);
    bindQueue(name, exchange, routingKey);
  }

  public void createQueue(String name, Map<String, Object> args) {
    execute(commands.queueDeclare(name, DURABLE, EXCLUSIVE, AUTO_DELETE, args));
  }

  public void bindQueue(String name, String exchange, String routingKey) {
    execute(commands.queueBind(name, exchange, routingKey));
  }

  public Long getMessageCount(String queue) {
    return execute(commands.messageCount(queue));
  }

  public boolean isChannelAvailable() {
    return channel.isOpen();
  }

  Channel getChannel() {
    return channel;
  }

  public AMQP.Queue.PurgeOk queuePurge(String name) {
    return execute(commands.queuePurge(name));
  }

  private <T> T execute(ChannelCommand<T> channelCommand) {
    // The channel might not be available or become unavailable due to a connection error. In this
    // case, we wait until the connection becomes available again.
    while (true) {
      if (channelAvailable) {
        try {
          return channelCommand.execute();
        } catch (IOException | AlreadyClosedException e) {
          if (e instanceof AlreadyClosedException && !((AlreadyClosedException) e).isHardError()) {
            recoverChannel();
          } else {
            log.warn(
                "Failed to communicate with RabbitMQ: '{}', waiting for channel to become available again",
                e.getMessage());
            channelAvailable = false;
          }
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

  private void recoverChannel() {
    try {
      connection.recoverChannel();
      this.channelListeners.forEach(ChannelListener::handleReset);
    } catch (IOException ex) {
      log.error("Failed to recreate RabbitMQ channel", ex);
      throw new RuntimeException(ex);
    }
  }

  public void addChannelListener(ChannelListener listener) {
    this.channelListeners.add(listener);
  }

  public void removeChannelListener(ChannelListener listener) {
    this.channelListeners.remove(listener);
  }
}
