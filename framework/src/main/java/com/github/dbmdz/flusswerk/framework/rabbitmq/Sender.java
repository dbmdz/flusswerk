package com.github.dbmdz.flusswerk.framework.rabbitmq;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import java.util.Collection;

/** High-level interface for sending messages to RabbitMQ. */
public interface Sender {
  void send(Message message) throws IOException;

  void send(Collection<Message> messages) throws IOException;

  void send(Message... messages) throws IOException;

  void sendRaw(byte[] message);
}
