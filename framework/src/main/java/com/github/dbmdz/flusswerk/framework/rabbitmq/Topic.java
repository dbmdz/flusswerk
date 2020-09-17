package com.github.dbmdz.flusswerk.framework.rabbitmq;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents a AMQP/RabbitMQ topic to send messages to. In many setups this is equal to the
 * respective queue name.
 */
public class Topic {

  private final String name;
  private final MessageBroker messageBroker;

  Topic(String name, MessageBroker messageBroker) {
    this.name = requireNonNull(name);
    this.messageBroker = messageBroker;
  }

  /**
   * Sends a message to this topic.
   *
   * @param message The message to send.
   * @throws IOException If communication with RabbitMQ fails or if the message cannot be serialized
   *     to JSON.
   */
  public void send(Message message) throws IOException {
    messageBroker.send(name, message);
  }

  /**
   * Sends multiple messages to this topic.
   *
   * @param messages The messages to send.
   * @throws IOException If communication with RabbitMQ fails or if the message cannot be serialized
   *     to JSON.
   */
  public void send(Collection<Message> messages) throws IOException {
    messageBroker.send(name, messages);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Topic) {
      Topic topic = (Topic) o;
      return Objects.equals(name, topic.name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return "Topic{name='" + name + "'}";
  }

  /** @return The name of this {@link Topic}. */
  public String getName() {
    return name;
  }
}
