package com.github.dbmdz.flusswerk.framework.rabbitmq;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Represents a AMQP/RabbitMQ topic to send messages to. In many setups this is equal to the
 * respective queue name.
 */
public class Topic {

  private final String name;
  private final MessageBroker messageBroker;
  private final Tracing tracing;

  Topic(String name, MessageBroker messageBroker, Tracing tracing) {
    this.name = requireNonNull(name);
    this.messageBroker = messageBroker;
    this.tracing = requireNonNull(tracing);
  }

  /**
   * Sends a message to this topic. The message will have a tracing id either based on the incoming
   * message or newly generated for applications that do not work on incoming messages. In that
   * case, every time you call this method it creates a new tracing path.
   *
   * @param message The message to send.
   * @throws IOException If communication with RabbitMQ fails or if the message cannot be serialized
   *     to JSON.
   */
  public void send(Message message) throws IOException {
    // Only set a tracing path if there is none yet
    if (message.getTracing() == null || message.getTracing().isEmpty()) {
      List<String> tracingPath = getTracingPath();
      message.setTracing(tracingPath);
    }
    messageBroker.send(name, message);
  }

  private List<String> getTracingPath() {
    List<String> tracingPath = tracing.tracingPath();
    if (tracingPath.isEmpty()) {
      // no tracing path available from tracing
      // => app does not operate on incoming messages
      // => needs a fresh tracing path
      tracingPath = tracing.newPath();
    }
    return tracingPath;
  }

  /**
   * Sends multiple messages to this topic.
   *
   * @param messages The messages to send.
   * @throws IOException If communication with RabbitMQ fails or if the message cannot be serialized
   *     to JSON.
   */
  public void send(Collection<Message> messages) throws IOException {
    // Get a new tracing path in case one is needed
    final List<String> tracingPath = getTracingPath();
    messages.stream()
        .filter(message -> message.getTracing() == null || message.getTracing().isEmpty())
        .forEach(message -> message.setTracing(tracingPath));
    messageBroker.send(name, messages);
  }

  public void send(Stream<Message> messages) {
    String tracingId = "123";
    messages.forEach(
        message -> {
          message.setTracingId(tracingId);
          try {
            messageBroker.send(name, message);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
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
