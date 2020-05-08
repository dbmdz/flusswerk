package com.github.dbmdz.flusswerk.framework.reporting;

import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.model.Message;

/** Functional interface to easily create custom reporters with a lambda. */
@FunctionalInterface
public interface ReportFunction extends ProcessReport {
  enum ReportType {
    SUCCESS,
    FAIL,
    FAIL_AFTER_MAX_RETRIES,
    REJECT
  }

  void report(ReportType type, Message msg, Exception e);

  @Override
  default void reportSuccess(Message msg) {
    this.report(ReportType.SUCCESS, msg, null);
  }

  @Override
  default void reportFail(Message msg, StopProcessingException e) {
    this.report(ReportType.FAIL, msg, e);
  }

  @Override
  default void reportFailAfterMaxRetries(Message msg, Exception e) {
    this.report(ReportType.FAIL_AFTER_MAX_RETRIES, msg, e);
  }

  @Override
  default void reportReject(Message msg, Exception e) {
    this.report(ReportType.REJECT, msg, e);
  }
}
