package com.github.dbmdz.flusswerk.framework.flow;

/**
 * Collect base metrics for a single flow - did the execution have errors and how long does it take?
 *
 * @see com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder
 * @see com.github.dbmdz.flusswerk.framework.flow.builder.SetConfigurationStep
 */
public class FlowStatus {

  public enum Status {
    SUCCESS,
    ERROR_RETRY,
    ERROR_STOP
  }

  private final long startTime;

  private long endTime;

  private Status status;

  public FlowStatus() {
    this.startTime = System.currentTimeMillis();
    this.status = Status.SUCCESS;
  }

  public void stop() {
    this.endTime = System.currentTimeMillis();
  }

  public Status getStatus() {
    return status;
  }

  void setStatus(Status status) {
    this.status = status;
  }

  public long duration() {
    return this.endTime - this.startTime;
  }
}
