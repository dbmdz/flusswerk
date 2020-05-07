package com.github.dbmdz.flusswerk.examples.plain;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.model.Message;

public class AppMessage extends Message {

  private final String id;

  public AppMessage(String id) {
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
    if (o instanceof AppMessage) {
      AppMessage that = (AppMessage) o;
      return id.equals(that.id);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
