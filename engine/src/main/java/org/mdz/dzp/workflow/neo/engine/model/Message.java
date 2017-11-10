package org.mdz.dzp.workflow.neo.engine.model;

import static java.util.Objects.requireNonNull;

public class Message {

  private String value;

  private boolean broken;

  protected Message() {}

  public Message(String value) {
    this.value = requireNonNull(value);
    this.broken = false;
  }

  public String getValue() {
    return value;
  }

  public void setBroken(boolean broken) {
    this.broken = broken;
  }

  public boolean isBroken() {
    return broken;
  }

}
