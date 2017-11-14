package org.mdz.dzp.workflow.neo.engine.model;

import static java.util.Objects.requireNonNull;

public class Message {

  private long deliveryTag;

  private int retries;

  private String body;

  private String value;

  private boolean broken;

  protected Message() {}

  public Message(String value) {
    this.value = requireNonNull(value);
    this.broken = false;
  }

  public String getValue() {
    return value;
  }

  public void setBroken(boolean broken) {
    this.broken = broken;
  }

  public boolean isBroken() {
    return broken;
  }

  public long getDeliveryTag() {
    return deliveryTag;
  }

  public String getBody() {
    return body;
  }

  public void setDeliveryTag(long deliveryTag) {
    this.deliveryTag = deliveryTag;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public int getRetries() {
    return retries;
  }

  public void setRetries(int retries) {
    this.retries = retries;
  }
}
