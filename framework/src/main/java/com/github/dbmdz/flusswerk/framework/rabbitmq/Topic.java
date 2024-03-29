package com.github.dbmdz.flusswerk.framework.rabbitmq;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents a AMQP/RabbitMQ topic to send messages to. In many setups this is equal to the
 * respective queue name.
 */
public class Topic implements Sender {

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
   */
  @Override
  public void send(Message message) {
    // Only set a tracing path if there is none yet
    if (message.getTracing() == null || message.getTracing().isEmpty()) {
      message.setTracing(getTracingPath());
    }
    messageBroker.send(name, message);
  }

  /**
   * Sends multiple messages to this topic.
   *
   * @param messages The messages to send.
   */
  @Override
  public void send(Collection<Message> messages) {
    // Get a new tracing path in case one is needed
    final List<String> tracingPath = getTracingPath();
    messages.stream()
        .filter(message -> message.getTracing() == null || message.getTracing().isEmpty())
        .forEach(message -> message.setTracing(tracingPath));
    messageBroker.send(name, messages);
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
    messageBroker.sendRaw(name, message);
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
