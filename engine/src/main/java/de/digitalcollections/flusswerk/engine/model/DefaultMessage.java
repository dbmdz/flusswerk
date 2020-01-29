package de.digitalcollections.flusswerk.engine.model;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

public class DefaultMessage extends FlusswerkMessage<String> {

  private Map<String, String> data;

  public DefaultMessage() {
    this(null);
  }

  public DefaultMessage(String id) {
    super(id);
    this.data = new HashMap<>();
  }

  public Map<String, String> getData() {
    return data;
  }

  public void setData(Map<String, String> data) {
    this.data = requireNonNull(data);
  }

  public DefaultMessage put(Map<String, String> data) {
    if (data != null) {
      for (Map.Entry<String, String> entry : data.entrySet()) {
        this.data.put(entry.getKey(), entry.getValue());
      }
    }
    return this;
  }

  public DefaultMessage put(String key, String value) {
    data.put(key, value);
    return this;
  }

  public String get(String key) {
    return data.get(key);
  }

  @Override
  public String toString() {
    return "Message{id=" + identifier + ", envelope=" + getEnvelope() + ", data=" + data + "}";
  }
}
