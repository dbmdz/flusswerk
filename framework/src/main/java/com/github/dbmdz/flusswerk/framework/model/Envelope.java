package com.github.dbmdz.flusswerk.framework.model;

import java.time.Instant;

/** Technical metadata all implementations of {@link Message} must have. */
public class Envelope {

  private String body;

  private long deliveryTag;

  private int retries;

  private Instant created;

  private String source;

  /** Default constructor setting the Envelope.timestamp to now. */
  public Envelope() {
    created = Instant.now();
  }

  /**
   * The original String representation before serializing into an {@link Message} instance. This
   * field will not be serialized when the message is sent.
   *
   * @return The original String representation of the {@link Message} as received from RabbitMQ.
   */
  public String getBody() {
    return body;
  }

  /**
   * The original String representation before serializing into an {@link Message} instance. This
   * field will not be serialized when the message is sent.
   *
   * @param body The string representation to set.
   */
  public void setBody(String body) {
    this.body = body;
  }

  /**
   * The delivery tag set by RabbitMQ for low level message handling.
   *
   * @return The delivery tag set by RabbitMQ.
   */
  public long getDeliveryTag() {
    return deliveryTag;
  }

  /**
   * The delivery tag set by RabbitMQ for low level message handling.
   *
   * @param deliveryTag The delivery tag set by RabbitMQ.
   */
  public void setDeliveryTag(long deliveryTag) {
    this.deliveryTag = deliveryTag;
  }

  /**
   * Gets the number of retries before a message is sent to the failed queue.
   *
   * @return The number of retries.
   */
  public int getRetries() {
    return retries;
  }

  /**
   * Sets the number of retries before a message is sent to the failed queue.
   *
   * @param retries The number of retries.
   */
  public void setRetries(int retries) {
    this.retries = retries;
  }

  /**
   * Gets the timestamp when the {@link Message} was created.
   *
   * @return The timestamp when the message was created.
   */
  public Instant getCreated() {
    return created;
  }

  /**
   * Sets the timestamp when the {@link Message} was created.
   *
   * @param created The timestamp when the message was created.
   */
  public void setCreated(Instant created) {
    this.created = created;
  }

  /**
   * Gets the queue name where this message was received from.
   *
   * @return the queue name where this message was received from.
   */
  public String getSource() {
    return source;
  }

  /**
   * Gets the queue name where this message was received from.
   *
   * @param source the queue name where this message was received from.
   */
  public void setSource(String source) {
    this.source = source;
  }

  @Override
  public String toString() {
    return "Envelope{deliveryTag=" + deliveryTag + ", retries=" + retries + "}";
  }
}
