package com.github.dbmdz.flusswerk.spring.boot.example.model;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.Objects;

public class Greeting extends Message {

  private final String id;

  private final String text;

  public Greeting(String id, String text) {
    this.id = requireNonNull(id);
    this.text = requireNonNull(text);
  }

  public String getText() {
    return text;
  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return "Greeting{id=" + id + ", envelope=" + getEnvelope() + ", text=" + text + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Greeting) {
      Greeting greeting = (Greeting) o;
      return id.equals(greeting.id) && text.equals(greeting.text);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, text);
  }
}
