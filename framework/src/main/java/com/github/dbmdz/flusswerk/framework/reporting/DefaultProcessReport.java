package com.github.dbmdz.flusswerk.framework.reporting;

import static java.util.Objects.requireNonNull;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultProcessReport implements ProcessReport {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProcessReport.class);

  private final String name;

  public DefaultProcessReport(String name) {
    this.name = requireNonNull(name);
  }

  @Override
  public void reportSuccess(Message message) {
    LOGGER.info("{} successful", name);
  }

  @Override
  public void reportFail(Message message, StopProcessingException e) {
    getLogger().error(
        "{} failed terminally ({}, {})",
        name,
        keyValue("message", message),
        keyValue("exception", e.toString()),
        e);
  }

  @Override
  public void reportFailAfterMaxRetries(Message message, Exception e) {
    getLogger().error(
        "{} failed after maximum number of retries ({}, {})",
        name,
        keyValue("message", message),
        keyValue("exception", e.toString()),
        e);
  }

  @Override
  public void reportReject(Message message, Exception e) {
    getLogger().warn(
        "{} rejected for retry ({}, {})",
        name,
        keyValue("message", message),
        keyValue("exception", e.toString()));
  }

  protected Logger getLogger() {
    return LOGGER;
  }
}
