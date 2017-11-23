package de.digitalcollections.workflow.engine.model;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class DefaultMessage implements Message<String> {

  private Meta meta;

  private Map<String, String> data;

  public DefaultMessage() {
    this(null, null);
  }

  protected DefaultMessage(String type) {
    this(type, null);
  }

  protected DefaultMessage(String type, String id) {
    this.meta = new Meta();
    this.data = new HashMap<>();
    this.put("type", type);
    this.put("id", id);
  }

  @Override
  public Meta getMeta() {
    return meta;
  }

  @Override
  public String getType() {
    return data.get("type");
  }

  public Map<String, String> getData() {
    return data;
  }

  public void setData(Map<String, String> data) {
    this.data = requireNonNull(data);
  }

  public DefaultMessage put(String key, String value) {
    data.put(key, value);
    return this;
  }

  public String get(String key) {
    return data.get(key);
  }

  @Override
  public String getId() {
    return data.get("id");
  }

  public static DefaultMessage withType(String type) {
    DefaultMessage message = new DefaultMessage();
    message.put("type", type);
    return message;
  }

  public static DefaultMessage withId(String id) {
    DefaultMessage message = new DefaultMessage();
    message.put("id", id);
    return message;
  }

  public DefaultMessage andType(String type) {
    this.put("type", type);
    return this;
  }

  public DefaultMessage andId(String id) {
    this.put("id", id);
    return this;
  }

}
