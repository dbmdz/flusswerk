package com.github.dbmdz.flusswerk.framework.flow;

import com.github.dbmdz.flusswerk.framework.exceptions.SkipProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.flow.builder.ConfigurationStep;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.monitoring.Status;
import java.time.Duration;

/**
 * Collect base metrics for a single flow - did the execution have errors and how long does it take?
 *
 * @see com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder
 * @see ConfigurationStep
 */
public class FlowInfo {

  private final long startTime;
  private long endTime;
  private Status status;
  private final Message message;

  public FlowInfo(Message message) {
    this.startTime = System.nanoTime();
    this.status = Status.SUCCESS;
    this.message = message;
  }

  public void stop() {
    this.endTime = System.nanoTime();
  }

  public Status getStatus() {
    return status;
  }

  void setStatusFrom(Exception e) {
    if (e instanceof SkipProcessingException) {
      status = Status.SKIP;
    } else if (e instanceof StopProcessingException) {
      status = Status.ERROR_STOP;
    } else {
      status = Status.ERROR_RETRY;
    }
  }

  public Message getMessage() {
    return message;
  }

  public Duration duration() {
    return Duration.ofNanos(this.endTime - this.startTime);
  }
}
