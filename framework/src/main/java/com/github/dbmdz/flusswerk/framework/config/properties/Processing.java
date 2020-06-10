package com.github.dbmdz.flusswerk.framework.config.properties;

import javax.validation.constraints.Min;

/** Configuration related to the processing. */
public class Processing {

  @Min(0)
  private final Integer maxRetries;

  @Min(1)
  private final Integer threads;

  public Processing(@Min(0) Integer maxRetries, @Min(1) Integer threads) {
    this.maxRetries = maxRetries;
    this.threads = threads;
  }

  /** @return the maximum number of retries before a message ends up in the failed queue. */
  public Integer getMaxRetries() {
    return maxRetries;
  }

  /** @return The number of concurrent processing threads in one job instance. */
  public Integer getThreads() {
    return threads;
  }

  @Override
  public String toString() {
    return StringRepresentation.of(Processing.class)
        .property("maxRetries", maxRetries)
        .property("threads", threads)
        .toString();
  }
}
