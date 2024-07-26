package com.github.dbmdz.flusswerk.framework.reporting;

import com.github.dbmdz.flusswerk.framework.exceptions.RetryProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.model.Message;

public interface ProcessReport {

  /**
   * Report a successfully handled message
   *
   * @param message The message, which was successfully handled
   */
  void reportSuccess(Message message);

  /**
   * Report a failed (finally failed) message
   *
   * @param message The message, which finally failed
   * @param e The exception, why the message failed
   */
  void reportFail(Message message, StopProcessingException e);

  /**
   * Report a failed message, which failed after the maximum number of retries
   *
   * @param message The message, which finally failed after the maximum number of retries
   * @param e The exception, why the message failed
   */
  void reportFailAfterMaxRetries(Message message, Exception e);

  /**
   * Report a rejected (temporarily failed) message
   *
   * @deprecated Use {@link #reportRetry(Message, RuntimeException)} instead
   * @param message The message, which temporarily failed
   * @param e The exception, why the message failed
   */
  @Deprecated(since = "6.0.0", forRemoval = true)
  void reportReject(Message message, Exception e);

  /**
   * Report a rejected (temporarily failed) message
   *
   * @param message The message, which temporarily failed
   * @param e The exception, why the message failed
   */
  default void reportRetry(Message message, RuntimeException e) {
    reportReject(message, e);
  }

  /**
   * Report that a message has been rejected to retry with the same or different messages and
   * potentially also has messages sent anyway.
   *
   * @param message The message, which temporarily failed
   * @param e The exception, why the message failed
   */
  default void reportComplexRetry(Message message, RetryProcessingException e) {
    reportReject(message, e);
  }

  /**
   * Report that a message has failed after the maximum number of retries and potentially also has
   * messages sent anyway.
   *
   * @param message The message which finally failed after the maximum number of retries
   * @param e The exception why the message failed
   */
  default void reportComplexFailedAfterMaxRetries(Message message, RetryProcessingException e) {
    reportReject(message, e);
  }

  default void reportSkip(Message message, Exception skip) {
    reportSuccess(message);
  }
}
