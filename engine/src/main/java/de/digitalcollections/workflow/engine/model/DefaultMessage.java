package de.digitalcollections.workflow.engine.model;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class DefaultMessage implements Message<String> {

  private Envelope envelope;

  private Map<String, String> data;

  public DefaultMessage() {
    this(null, null);
  }

  protected DefaultMessage(String type) {
    this(type, null);
  }

  protected DefaultMessage(String type, String id) {
    this.envelope = new Envelope();
    this.data = new HashMap<>();
    this.put("type", type);
    this.put("id", id);
  }

  @Override
  public Envelope getEnvelope() {
    return envelope;
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

  public static DefaultMessage withId(String id) {
    DefaultMessage message = new DefaultMessage();
    message.put("id", id);
    return message;
  }

  @Override
  public String toString() {
    return "Message{envelope=" + envelope + ", data=" + data + "}";
  }
}
