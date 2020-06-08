package com.github.dbmdz.flusswerk.framework.reporting;

import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.model.Message;

/** Empty implementation for unit tests that reports nothing. */
public class SilentProcessReport implements ProcessReport {

  @Override
  public void reportSuccess(Message message) {}

  @Override
  public void reportFail(Message message, StopProcessingException e) {}

  @Override
  public void reportFailAfterMaxRetries(Message message, Exception e) {}

  @Override
  public void reportReject(Message message, Exception e) {}
}
