package de.digitalcollections.workflow.engine.messagebroker;

import de.digitalcollections.workflow.engine.exceptions.WorkflowSetupException;

import static java.util.Objects.requireNonNull;

class RoutingConfig {


  private String exchange;

  private String deadLetterExchange;

  private String readFrom;

  private String writeTo;

  private String failedQueue;

  private String retryQueue;

  public RoutingConfig() {
    readFrom = null;
    writeTo = null;
  }

  public void complete() {
    if (exchange == null) {
      exchange = "workflow";
    }
    if (deadLetterExchange == null) {
      deadLetterExchange = exchange + ".retry";
    }
    if (readFrom == null) {
      throw new WorkflowSetupException("A workflow always needs an input queue. Please configure 'readFrom'.");
    }
    if (failedQueue == null) {
      failedQueue = readFrom + ".failed";
    }
    if (retryQueue == null) {
      retryQueue = readFrom + ".retry";
    }
  }

  public String getExchange() {
    return exchange;
  }

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }

  public String getDeadLetterExchange() {
    return deadLetterExchange;
  }

  public void setDeadLetterExchange(String deadLetterExchange) {
    this.deadLetterExchange = deadLetterExchange;
  }

  public String getReadFrom() {
    return readFrom;
  }

  public void setReadFrom(String readFrom) {
    this.readFrom = requireNonNull(readFrom);

  }

  public String getWriteTo() {
    return writeTo;
  }

  public void setWriteTo(String writeTo) {
    this.writeTo = writeTo;
  }

  public boolean hasWriteTo() {
    return writeTo != null;
  }

  public String getFailedQueue() {
    return failedQueue;
  }

  public void setFailedQueue(String failedQueue) {
    this.failedQueue = failedQueue;
  }

  public String getRetryQueue() {
    return retryQueue;
  }

  public void setRetryQueue(String retryQueue) {
    this.retryQueue = retryQueue;
  }

  public boolean hasFailedQueue() {
    return failedQueue != null;
  }
}
