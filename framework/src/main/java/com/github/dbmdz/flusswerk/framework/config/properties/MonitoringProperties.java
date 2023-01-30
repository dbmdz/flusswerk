package com.github.dbmdz.flusswerk.framework.config.properties;

import static java.util.Objects.requireNonNullElse;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings for monitoring endpoints. */
@ConfigurationProperties(prefix = "flusswerk.monitoring")
public record MonitoringProperties(String prefix) {
  public MonitoringProperties {
    prefix = requireNonNullElse(prefix, "flusswerk");
  }

  public static MonitoringProperties defaults() {
    return new MonitoringProperties(null); // use null because constructor sets defaults
  }
}
