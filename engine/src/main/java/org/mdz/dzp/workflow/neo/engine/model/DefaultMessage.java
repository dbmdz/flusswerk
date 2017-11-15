package org.mdz.dzp.workflow.neo.engine.model;

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

  public DefaultMessage() {
    parameters = new HashMap<>();
  }


  public DefaultMessage(String type) {
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

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }


}
