package dev.mdz.flusswerk.config.properties;

import static java.util.Objects.requireNonNullElse;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration related to the processing. */
@ConfigurationProperties(prefix = "flusswerk.processing")
public record ProcessingProperties(@Min(1) Integer threads) {

  public ProcessingProperties {
    threads = requireNonNullElse(threads, 5);
  }

  public static ProcessingProperties defaults() {
    return new ProcessingProperties(null); // use null so constructor sets default values
  }
}
