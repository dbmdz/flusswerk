package dev.mdz.flusswerk.rabbitmq;

import dev.mdz.flusswerk.model.Message;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Flusswerk-specific abstraction, collection of topics/queues. No equivalent in RabbitMQ. */
public class Route implements Sender {
  private final String name;
  private final List<Topic> topics;

  public Route(String name) {
    this.name = name;
    topics = new ArrayList<>();
  }

  public Route(String name, List<Topic> topics) {
    this.name = name;
    this.topics = topics;
  }

  public void addTopic(Topic topic) {
    this.topics.add(topic);
  }

  /**
   * Sends a message to the topics on this route.
   *
   * @param message The message to send.
   */
  @Override
  public void send(Message message) {
    for (Topic topic : topics) {
      topic.send(message);
    }
  }

  /**
   * Sends multiple messages to the topics on this route.
   *
   * @param messages The messages to send.
   */
  @Override
  public void send(Collection<Message> messages) {
    for (Topic topic : topics) {
      topic.send(messages);
    }
  }

  /**
   * Convenience implementation, mostly for tests.
   *
   * @param messages The messages to send.
   */
  @Override
  public void send(Message... messages) {
    send(List.of(messages));
  }

  /**
   * Sends a bunch of bytes to RabbitMQ.
   *
   * <p><b>Use with caution and only when using {@link Message} is not viable.</b>
   *
   * @param message The message serialized to bytes
   */
  @Override
  public void sendRaw(byte[] message) {
    for (Topic topic : topics) {
      topic.sendRaw(message);
    }
  }

  public String getName() {
    return this.name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Route route) {
      return Objects.equals(topics, route.topics);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(topics);
  }

  @Override
  public String toString() {
    return "Route{name='" + name + "', topics='" + topics + "'}";
  }
}
