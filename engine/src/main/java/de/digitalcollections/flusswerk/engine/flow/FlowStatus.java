package de.digitalcollections.flusswerk.engine.flow;

import java.util.function.Consumer;

/**
 * Collect base metrics for a single flow - did the execution have errors and how long does it take?
 *
 * <p>Used for metrics handlers in {@link FlowBuilder#monitor(Consumer)}
 */
public class FlowStatus {

  public enum Status {
    SUCCESS,
    ERROR_RETRY,
    ERROR_STOP
  }

  private long startTime;

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
