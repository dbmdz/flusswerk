package dev.mdz.flusswerk.rabbitmq;

import com.rabbitmq.client.*;
import java.util.Map;

public class ChannelCommands {

  private final Channel channel;

  public ChannelCommands(Channel channel) {
    this.channel = channel;
  }

  public ChannelCommand<Void> basicPublish(
      String exchange, String routingKey, AMQP.BasicProperties properties, byte[] data) {
    return () -> {
      channel.basicPublish(exchange, routingKey, properties, data);
      return null;
    };
  }

  public ChannelCommand<Void> basicAck(long deliveryTag, boolean multiple) {
    return () -> {
      channel.basicAck(deliveryTag, multiple);
      return null;
    };
  }

  public ChannelCommand<Void> basicReject(long deliveryTag, boolean requeue) {
    return () -> {
      channel.basicReject(deliveryTag, requeue);
      return null;
    };
  }

  public ChannelCommand<GetResponse> basicGet(String queue, boolean autoAck) {
    return () -> channel.basicGet(queue, autoAck);
  }

  public ChannelCommand<Void> basicConsume(String queue, boolean autoAck, Consumer consumer) {
    return () -> {
      channel.basicConsume(queue, autoAck, consumer);
      return null;
    };
  }

  public ChannelCommand<Void> exchangeDeclare(
      String exchange, BuiltinExchangeType type, boolean durable) {
    return () -> {
      channel.exchangeDeclare(exchange, type, durable);
      return null;
    };
  }

  public ChannelCommand<Void> queueDeclare(
      String name,
      boolean durable,
      boolean exclusive,
      boolean autoDelete,
      Map<String, Object> args) {
    return () -> {
      channel.queueDeclare(name, durable, exclusive, autoDelete, args);
      return null;
    };
  }

  public ChannelCommand<Void> queueBind(String name, String exchange, String routingKey) {
    return () -> {
      channel.queueBind(name, exchange, routingKey);
      return null;
    };
  }

  public ChannelCommand<Long> messageCount(String queue) {
    return () -> channel.messageCount(queue);
  }

  public ChannelCommand<AMQP.Queue.PurgeOk> queuePurge(String queue) {
    return () -> channel.queuePurge(queue);
  }
}
