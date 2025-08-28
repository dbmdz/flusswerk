package dev.mdz.flusswerk.rabbitmq;

import static java.util.Objects.requireNonNullElse;

import java.time.Duration;

public class FailurePolicy {

  private final String inputQueue;

  private final int retries;

  private final Duration backoff;

  private final String failedRoutingKey;

  private final String retryRoutingKey;

  public FailurePolicy(String inputQueue) {
    this(inputQueue, 5);
  }

  public FailurePolicy(String inputQueue, int retries) {
    this(inputQueue, null, null, retries, null);
  }

  public FailurePolicy(
      String inputQueue,
      String retryRoutingKey,
      String failedRoutingKey,
      Integer retries,
      Duration backoff) {
    this.inputQueue = inputQueue;
    this.retryRoutingKey = requireNonNullElse(retryRoutingKey, inputQueue + ".retry");
    this.failedRoutingKey = requireNonNullElse(failedRoutingKey, inputQueue + ".failed");
    this.retries = requireNonNullElse(retries, 5);
    this.backoff = requireNonNullElse(backoff, Duration.ofSeconds(30));
  }

  public String getInputQueue() {
    return inputQueue;
  }

  public int getRetries() {
    return retries;
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
