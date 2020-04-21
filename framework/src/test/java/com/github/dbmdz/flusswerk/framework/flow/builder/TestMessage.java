package com.github.dbmdz.flusswerk.framework.flow.builder;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.model.FlusswerkMessage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class TestMessage extends FlusswerkMessage<String> {

  private List<String> values;

  public TestMessage(String id) {
    super(requireNonNull(id));
    this.values = Collections.emptyList();
  }

  public TestMessage(String id, String... values) {
    super(requireNonNull(id));
    this.values = Arrays.asList(values);
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
