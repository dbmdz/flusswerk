package com.github.dbmdz.flusswerk.framework.messagebroker;

public class FailurePolicy {

  private final String inputQueue;

  private final int maxRetries;

  private final String failedRoutingKey;

  private final String retryRoutingKey;

  public FailurePolicy(String inputQueue) {
    this(inputQueue, 5);
  }

  public FailurePolicy(String inputQueue, int maxRetries) {
    this(inputQueue, inputQueue + ".retry", inputQueue + ".failed", maxRetries);
  }

  public FailurePolicy(
      String inputQueue, String retryRoutingKey, String failedRoutingKey, int maxRetries) {
    this.inputQueue = inputQueue;
    this.retryRoutingKey = retryRoutingKey;
    this.failedRoutingKey = failedRoutingKey;
    this.maxRetries = maxRetries;
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
