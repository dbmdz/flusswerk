package com.github.dbmdz.flusswerk.framework.config.properties;

import static java.util.Objects.requireNonNullElse;

import javax.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.yaml.snakeyaml.Yaml;

/** Configuration related to the processing. */
@ConstructorBinding
@ConfigurationProperties(prefix = "flusswerk.processing")
public class ProcessingProperties {

  @Min(1)
  private final Integer threads;

  public ProcessingProperties(@Min(1) Integer threads) {
    this.threads = requireNonNullElse(threads, 5);
  }

  /** @return The number of concurrent processing threads in one job instance. */
  public Integer getThreads() {
    return threads;
  }

  @Override
  public String toString() {
    Yaml yaml = new Yaml();
    return yaml.dump(this);
  }

  public static ProcessingProperties defaults() {
    return new ProcessingProperties(null); // use null so constructor sets default values
  }
}
