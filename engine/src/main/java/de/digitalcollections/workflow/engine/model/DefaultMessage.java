package de.digitalcollections.workflow.engine.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class DefaultMessage implements Message<String> {

  private Map<String, String> parameters;

  private long deliveryTag;

  private int retries;

  private String body;

  private String type;

  private String id;

  private LocalDateTime timestamp;

  public DefaultMessage() {
    this(null, null);
  }

  public DefaultMessage(String type) {
    this(type, null);
  }

  public DefaultMessage(String type, String id) {
    this.type = type;
    this.id = id;
    this.parameters = new HashMap<>();
    this.timestamp = LocalDateTime.now();
  }

  @Override
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public long getDeliveryTag() {
    return deliveryTag;
  }

  @Override
  public void setDeliveryTag(long deliveryTag) {
    this.deliveryTag = deliveryTag;
  }

  @Override
  public String getBody() {
    return body;
  }

  @Override
  public void setBody(String body) {
    this.body = body;
  }

  @Override
  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  @Override
  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public int getRetries() {
    return retries;
  }

  @Override
  public void setRetries(int retries) {
    this.retries = retries;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = requireNonNull(parameters);
  }

  public DefaultMessage put(String key, String value) {
    parameters.put(key, value);
    return this;
  }

  public String get(String key) {
    return parameters.get(key);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

}
