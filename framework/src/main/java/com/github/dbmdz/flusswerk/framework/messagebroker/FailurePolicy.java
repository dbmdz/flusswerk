package com.github.dbmdz.flusswerk.framework.messagebroker;

import static java.util.Objects.requireNonNullElse;

import java.time.Duration;

public class FailurePolicy {

  private final String inputQueue;

  private final int maxRetries;

  private final Duration backoff;

  private final String failedRoutingKey;

  private final String retryRoutingKey;

  public FailurePolicy(String inputQueue) {
    this(inputQueue, 5);
  }

  public FailurePolicy(String inputQueue, int maxRetries) {
    this(inputQueue, null, null, maxRetries, null);
  }

  public FailurePolicy(
      String inputQueue,
      String retryRoutingKey,
      String failedRoutingKey,
      Integer maxRetries,
      Integer durationMs) {
    this.inputQueue = inputQueue;
    this.retryRoutingKey = requireNonNullElse(retryRoutingKey, inputQueue + ".retry");
    this.failedRoutingKey = requireNonNullElse(failedRoutingKey, inputQueue + ".failed");
    this.maxRetries = requireNonNullElse(maxRetries, 5);
    this.backoff = Duration.ofMillis(requireNonNullElse(durationMs, 30_000));
  }

  public String getInputQueue() {
    return inputQueue;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public String getFailedRoutingKey() {
    return failedRoutingKey;
  }

  public String getRetryRoutingKey() {
    return retryRoutingKey;
  }

  public Duration getBackoff() {
    return backoff;
  }
}
