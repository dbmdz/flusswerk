package org.mdz.dzp.workflow.neo.engine;

import java.util.HashMap;
import java.util.Map;
import org.mdz.dzp.workflow.neo.engine.model.Message;

import static java.util.Objects.requireNonNull;

public class CustomMessage implements Message<Long> {

  private Map<String, String> parameters;

  private long deliveryTag;

  private int retries;

  private String body;

  private String type;

  private Long id;

  private String customField;

  public CustomMessage() {
    parameters = new HashMap<>();
  }


  public CustomMessage(String type) {
    this.type = type;
    parameters = new HashMap<>();
  }

  public String getType() {
    return type;
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

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = requireNonNull(parameters);
  }

  public void put(String key, String value) {
    parameters.put(key, value);
  }

  public String get(String key) {
    return parameters.get(type);
  }

  public void setType(String type) {
    this.type = type;
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
