package com.github.dbmdz.flusswerk.framework.config.properties;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "spring.application")
public class AppProperties {

  private final String name;

  public AppProperties(@NotEmpty String name) {
    if (!StringUtils.hasText(name)) {
      throw new RuntimeException(
          "Any Flusswerk application needs to define spring.application.name");
    }
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return StringRepresentation.of(this);
  }
}
