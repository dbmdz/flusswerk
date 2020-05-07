package com.github.dbmdz.flusswerk.framework;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.HashMap;
import java.util.Map;

public class CustomMessage extends Message {

  private Map<String, String> data;

  private String customField;

  private Long id;

  public CustomMessage() {
    data = new HashMap<>();
  }

  public CustomMessage(Long id) {
    this.data = new HashMap<>();
    this.id = id;
  }

  public Map<String, String> getData() {
    return data;
  }

  public void setData(Map<String, String> data) {
    this.data = requireNonNull(data);
  }

  public CustomMessage put(String key, String value) {
    data.put(key, value);
    return this;
  }

  public String get(String key) {
    return data.get(key);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCustomField() {
    return customField;
  }

  public void setCustomField(String customField) {
    this.customField = customField;
  }
}
