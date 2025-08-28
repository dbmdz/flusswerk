package dev.mdz.flusswerk.config.properties;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "spring.application")
public record AppProperties(@NotEmpty String name) {

  public AppProperties {
    if (!StringUtils.hasText(name)) {
      throw new RuntimeException(
          "Any Flusswerk application needs to define spring.application.name");
    }
  }
}
