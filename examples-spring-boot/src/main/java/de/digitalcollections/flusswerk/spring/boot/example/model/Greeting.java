package de.digitalcollections.flusswerk.spring.boot.example.model;

import de.digitalcollections.flusswerk.engine.model.Envelope;
import de.digitalcollections.flusswerk.engine.model.Message;

public class Greeting implements Message<Integer> {
  private Envelope envelope;

  private Integer id;

  private String text;

  protected Greeting() {
    this(null, null);
  }

  public Greeting(Integer id, String text) {
    this.envelope = new Envelope();
    this.id = id;
    this.text = text;
  }

  @Override
  public Envelope getEnvelope() {
    return envelope;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public Integer getId() {
    return id;
  }

  @Override
  public String toString() {
    return "Greeting{id=" + id + ", envelope=" + envelope + ", text=" + text + "}";
  }
}
