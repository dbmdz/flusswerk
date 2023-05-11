package com.github.dbmdz.flusswerk.framework.rabbitmq;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents a AMQP/RabbitMQ topic to send messages to. In many setups this is equal to the
 * respective queue name.
 */
public class Topic {

  private final String name;
  private final String exchange;
  private final Tracing tracing;
  private final RabbitClient rabbitClient;

  Topic(String name, String exchange, RabbitClient rabbitClient, Tracing tracing) {
    this.name = requireNonNull(name);
    this.exchange = requireNonNull(exchange);
    this.tracing = requireNonNull(tracing);
    this.rabbitClient = requireNonNull(rabbitClient);
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
      message.setTracing(getTracingPath());
    }
    rabbitClient.send(exchange, name, message);
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
    for (Message message : messages) {
      rabbitClient.send(exchange, name, message);
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
    rabbitClient.sendRaw(exchange, name, message);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Topic topic) {
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

  /**
   * @return The name of this {@link Topic}.
   */
  public String getName() {
    return name;
  }
}
