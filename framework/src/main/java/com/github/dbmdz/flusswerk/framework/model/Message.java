package com.github.dbmdz.flusswerk.framework.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A generic message as it will be sent over RabbitMQ. */
public class Message {

  private final Envelope envelope;

  private List<String> tracing;

  public Message() {
    this.envelope = new Envelope();
    this.tracing = Collections.emptyList();
  }

  /**
   * Technical metadata like timestamps and retries.
   *
   * @return An object containing the messages technical metadata.
   */
  public Envelope getEnvelope() {
    return envelope;
  }

  public List<String> getTracing() {
    return tracing;
  }

  public void setTracing(List<String> tracing) {
    this.tracing = Objects.requireNonNullElseGet(tracing, Collections::emptyList);
  }

  @Override
  public String toString() {
    return String.format("Message{hashcode=%s, tracing=%s}", hashCode(), tracing);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Message message = (Message) o;
    return tracing.equals(message.getTracing());
  }

  @Override
  public int hashCode() {
    return Objects.hash(tracing);
  }
}
