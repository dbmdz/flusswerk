package com.github.dbmdz.flusswerk.framework.messagebroker;

import static java.util.Objects.requireNonNullElse;

public class FailurePolicy {

  private final String inputQueue;

  private final int maxRetries;

  private final String failedRoutingKey;

  private final String retryRoutingKey;

  public FailurePolicy(String inputQueue) {
    this(inputQueue, 5);
  }

  public FailurePolicy(String inputQueue, int maxRetries) {
    this(inputQueue, null, null, maxRetries);
  }

  public FailurePolicy(
      String inputQueue, String retryRoutingKey, String failedRoutingKey, Integer maxRetries) {
    this.inputQueue = inputQueue;
    this.retryRoutingKey = requireNonNullElse(retryRoutingKey, inputQueue + ".retry");
    this.failedRoutingKey = requireNonNullElse(failedRoutingKey, inputQueue + ".failed");
    this.maxRetries = requireNonNullElse(maxRetries, 5);
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
}
