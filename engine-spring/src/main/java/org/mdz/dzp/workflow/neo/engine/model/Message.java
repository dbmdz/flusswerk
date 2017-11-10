package org.mdz.dzp.workflow.neo.engine.model;

import static java.util.Objects.requireNonNull;

public class Message {

  private String value;

  public Message(String value) {
    this.value = requireNonNull(value);
  }

  public String getValue() {
    return value;
  }

}
