package com.github.dbmdz.flusswerk.framework.messagebroker;

public class FailurePolicy {

  private final String inputQueue;

  private final int maxRetries;

  private String failedRoutingKey;

  private String retryRoutingKey;

  public FailurePolicy(String inputQueue) {
    this(inputQueue, 5);
  }

  public FailurePolicy(String inputQueue, int maxRetries) {
    this.maxRetries = maxRetries;
    this.inputQueue = inputQueue;
    this.failedRoutingKey = inputQueue + ".failed";
    this.retryRoutingKey = inputQueue + ".retry";
  }

  public FailurePolicy(
      String inputQueue, String retryRoutingKey, String failedRoutingKey, int maxRetries) {
    this(inputQueue, maxRetries);
    this.failedRoutingKey = failedRoutingKey;
    this.retryRoutingKey = retryRoutingKey;
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
