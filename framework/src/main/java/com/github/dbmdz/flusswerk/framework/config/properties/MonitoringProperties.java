package com.github.dbmdz.flusswerk.framework.config.properties;

import java.util.Objects;

/** Settings for monitoring endpoints. */
public class MonitoringProperties {

  private final String prefix;

  public MonitoringProperties(String prefix) {
    this.prefix = Objects.requireNonNullElse(prefix, "flusswerk");
  }

  public String getPrefix() {
    return prefix;
  }

  @Override
  public String toString() {
    return StringRepresentation.of(MonitoringProperties.class)
        .property("prefix", prefix)
        .toString();
  }

  public static MonitoringProperties defaults() {
    return new MonitoringProperties(null); // use null because constructor sets defaults
  }
}
