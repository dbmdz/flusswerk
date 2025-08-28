package dev.mdz.flusswerk.rabbitmq;

import dev.mdz.flusswerk.model.Message;
import java.util.Collection;

/** High-level interface for sending messages to RabbitMQ. */
public interface Sender {
  void send(Message message);

  void send(Collection<Message> messages);

  void send(Message... messages);

  void sendRaw(byte[] message);
}
