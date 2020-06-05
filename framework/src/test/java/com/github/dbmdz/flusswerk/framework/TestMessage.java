package com.github.dbmdz.flusswerk.framework;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class TestMessage extends Message {

  private final String id;

  private final List<String> values;

  public TestMessage(String id) {
    this.id = requireNonNull(id);
    this.values = Collections.emptyList();
  }

  @JsonCreator
  public TestMessage(@JsonProperty("id") String id, @JsonProperty("values") String... values) {
    this.id = requireNonNull(id);
    if (values == null) {
      this.values = new ArrayList<>();
    } else {
      this.values = Arrays.asList(values);
    }
  }

  public String getId() {
    return id;
  }

  public List<String> getValues() {
    return values;
  }

  public Stream<String> values() {
    return values.stream();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof TestMessage) {
      TestMessage other = (TestMessage) o;
      return id.equals(other.id) && values.equals(other.values);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, values);
  }

  @Override
  public String toString() {
    return "TestMessage{" + "values=" + values + ", id=" + id + '}';
  }
}
