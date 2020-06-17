package com.github.dbmdz.flusswerk.framework.config.properties;

import java.util.Objects;

/** Settings for monitoring endpoints. */
public class Monitoring {

  private final String prefix;

  public Monitoring(String prefix) {
    this.prefix = Objects.requireNonNullElse(prefix, "flusswerk");
  }

  public String getPrefix() {
    return prefix;
  }

  @Override
  public String toString() {
    return StringRepresentation.of(Monitoring.class).property("prefix", prefix).toString();
  }
}
