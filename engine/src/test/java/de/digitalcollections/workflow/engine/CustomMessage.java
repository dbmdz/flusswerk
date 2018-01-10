package de.digitalcollections.workflow.engine;

import de.digitalcollections.workflow.engine.model.Envelope;
import de.digitalcollections.workflow.engine.model.Message;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class CustomMessage implements Message<Long> {

  private Envelope envelope;

  private Map<String, String> data;

  private String customField;

  private Long id;

  public CustomMessage() {
    data = new HashMap<>();
  }


  public CustomMessage(Long id) {
    this.envelope = new Envelope();
    this.data = new HashMap<>();
    this.id = id;
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

  public CustomMessage put(String key, String value) {
    data.put(key, value);
    return this;
  }

  public String get(String key) {
    return data.get(key);
  }

  @Override
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
