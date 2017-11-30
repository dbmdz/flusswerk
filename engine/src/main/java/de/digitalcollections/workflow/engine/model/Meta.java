package de.digitalcollections.workflow.engine.model;

import java.time.LocalDateTime;

/**
 * Technical metadata all implementations of {@link Message} must have.
 */
public class Meta {

  private String body;

  private long deliveryTag;

  private int retries;

  private LocalDateTime timestamp;

  /**
   * Default constructor setting the Meta.timestamp to now.
   */
  public Meta() {
    timestamp = LocalDateTime.now();
  }

  /**
   * The original String representation before serializing into an {@link Message} instance. This field will not be serialized when the message is sent.
   *
   * @return The original String representation of the {@link Message} as received from RabbitMQ.
   */
  public String getBody() {
    return body;
  }

  /**
   * The original String representation before serializing into an {@link Message} instance. This field will not be serialized when the message is sent.
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
  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  /**
   * Sets the timestamp when the {@link Message} was created.
   *
   * @param timestamp The timestamp when the message was created.
   */
  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public String toString() {
    return "Meta{deliveryTag=" + deliveryTag + ", retries=" + retries + "}";
  }
}
