package com.github.dbmdz.flusswerk.framework.rabbitmq;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Flusswerk-specific abstraction, collection of topics/queues. No equivalent in RabbitMQ. */
public class Route {
  private String name;
  private List<Topic> topics;

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
   * Sends a message to the topics on this route. The message will have a tracing id either based on
   * the incoming message or newly generated for applications that do not work on incoming messages.
   * In that case, every time you call this method it creates a new tracing path.
   *
   * @param message The message to send.
   * @throws IOException If communication with RabbitMQ fails or if the message cannot be serialized
   *     to JSON.
   */
  public void send(Message message) throws IOException {
    for (Topic topic : topics) {
      topic.send(message);
    }
  }

  /**
   * Sends multiple messages to the topics on this route.
   *
   * @param messages The messages to send.
   * @throws IOException If communication with RabbitMQ fails or if the message cannot be serialized
   *     to JSON.
   */
  public void send(Collection<Message> messages) throws IOException {
    for (Topic topic : topics) {
      topic.send(messages);
    }
  }

  /**
   * Convenience implementation, mostly for tests.
   *
   * @param messages The messages to send.
   * @throws IOException If communication with RabbitMQ fails or if the message cannot be serialized
   *     to JSON.
   */
  public void send(Message... messages) throws IOException {
    send(List.of(messages));
  }

  /**
   * Sends a bunch of bytes to RabbitMQ.
   *
   * <p><b>Use with caution and only when using {@link Message} is not viable.</b>
   *
   * @param message The message serialized to bytes
   */
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
