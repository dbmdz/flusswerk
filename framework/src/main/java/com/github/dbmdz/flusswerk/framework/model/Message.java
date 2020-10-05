package com.github.dbmdz.flusswerk.framework.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A generic message as it will be sent over RabbitMQ. */
public class Message {

  private final Envelope envelope;

  private String tracingId;
  private List<String> tracing;

  public Message() {
    this.envelope = new Envelope();
    this.tracingId = null;
    this.tracing = Collections.emptyList();
  }

  public Message(String tracingId) {
    this.envelope = new Envelope();
    this.tracingId = tracingId;
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

  public void setTracingId(String tracingId) {
    this.tracingId = tracingId;
  }

  /**
   * Tracing ids allow to follow the processing of one object across multiple workflow jobs,
   *
   * @return the tracing id
   */
  public String getTracingId() {
    return tracingId;
  }

  public List<String> getTracing() {
    return tracing;
  }

  public void setTracing(List<String> tracing) {
    this.tracing = Objects.requireNonNullElseGet(tracing, Collections::emptyList);
  }

  @Override
  public String toString() {
    return String.format("Message{hashcode=%s, tracingId=%s}", hashCode(), tracingId);
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
    return Objects.equals(tracingId, message.tracingId) && tracing.equals(message.getTracing());
  }

  @Override
  public int hashCode() {
    return Objects.hash(tracingId, tracing);
  }
}
