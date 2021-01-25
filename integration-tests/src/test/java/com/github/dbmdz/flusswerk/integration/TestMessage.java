package com.github.dbmdz.flusswerk.integration;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.Objects;

public class TestMessage extends Message {

  private final String id;

  public TestMessage(@JsonProperty("id") String id) {
    this.id = requireNonNull(id);
  }

  public String getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestMessage that = (TestMessage) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "TestMessage{" + "id='" + id + '\'' + '}';
  }
}
