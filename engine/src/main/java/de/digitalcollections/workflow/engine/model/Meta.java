package de.digitalcollections.workflow.engine.model;

import java.time.LocalDateTime;

public class Meta {

  private String body;

  private long deliveryTag;

  private int retries;

  private LocalDateTime timestamp;

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public long getDeliveryTag() {
    return deliveryTag;
  }

  public void setDeliveryTag(long deliveryTag) {
    this.deliveryTag = deliveryTag;
  }

  public int getRetries() {
    return retries;
  }

  public void setRetries(int retries) {
    this.retries = retries;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

}
