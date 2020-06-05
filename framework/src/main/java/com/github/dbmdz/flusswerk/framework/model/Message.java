package com.github.dbmdz.flusswerk.framework.model;

/** A generic message as it will be sent over RabbitMQ. */
public class Message {

  private final Envelope envelope;

  private String tracingId;

  public Message() {
    this.envelope = new Envelope();
    this.tracingId = null;
  }

  public Message(String tracingId) {
    this.envelope = new Envelope();
    this.tracingId = tracingId;
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

  @Override
  public String toString() {
    return String.format("Message{hashcode=%s, tracingId=%s", hashCode(), tracingId);
  }
}
