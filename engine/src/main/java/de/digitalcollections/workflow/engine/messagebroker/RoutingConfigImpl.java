package de.digitalcollections.workflow.engine.messagebroker;

import de.digitalcollections.workflow.engine.exceptions.WorkflowSetupException;

import static java.util.Objects.requireNonNull;

class RoutingConfigImpl implements RoutingConfig {

  private String exchange;

  private String deadLetterExchange;

  private String readFrom;

  private String writeTo;

  private String failedQueue;

  private String retryQueue;

  public RoutingConfigImpl() {
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

  @Override
  public String getExchange() {
    return exchange;
  }

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }

  @Override
  public String getDeadLetterExchange() {
    return deadLetterExchange;
  }

  public void setDeadLetterExchange(String deadLetterExchange) {
    this.deadLetterExchange = deadLetterExchange;
  }

  @Override
  public String getReadFrom() {
    return readFrom;
  }

  public void setReadFrom(String readFrom) {
    this.readFrom = requireNonNull(readFrom);

  }

  @Override
  public String getWriteTo() {
    return writeTo;
  }

  public void setWriteTo(String writeTo) {
    this.writeTo = writeTo;
  }

  @Override
  public boolean hasWriteTo() {
    return writeTo != null;
  }

  @Override
  public String getFailedQueue() {
    return failedQueue;
  }

  public void setFailedQueue(String failedQueue) {
    this.failedQueue = failedQueue;
  }

  @Override
  public String getRetryQueue() {
    return retryQueue;
  }

  public void setRetryQueue(String retryQueue) {
    this.retryQueue = retryQueue;
  }

  @Override
  public boolean hasFailedQueue() {
    return failedQueue != null;
  }
}
