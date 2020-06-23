package com.github.dbmdz.flusswerk.framework.config.properties;

import static java.util.Objects.requireNonNullElse;

import javax.validation.constraints.Min;
import org.springframework.boot.context.properties.ConstructorBinding;

/** Configuration related to the processing. */
@ConstructorBinding
public class Processing {

  @Min(1)
  private final Integer threads;

  public Processing(@Min(1) Integer threads) {
    this.threads = requireNonNullElse(threads, 5);
  }

  /** @return The number of concurrent processing threads in one job instance. */
  public Integer getThreads() {
    return threads;
  }

  @Override
  public String toString() {
    return StringRepresentation.of(Processing.class).property("threads", threads).toString();
  }

  public static Processing defaults() {
    return new Processing(null); // use null so constructor sets default values
  }
}
