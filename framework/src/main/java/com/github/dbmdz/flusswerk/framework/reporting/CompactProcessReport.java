package com.github.dbmdz.flusswerk.framework.reporting;

import static java.util.Objects.requireNonNull;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple compact {@link ProcessReport} for structured messages. Requires the optional
 * logstash-logback dependency.
 */
public class CompactProcessReport implements ProcessReport {

  private static final Logger LOGGER = LoggerFactory.getLogger(CompactProcessReport.class);

  private final String name;

  public CompactProcessReport(String name) {
    this.name = requireNonNull(name);
  }

  @Override
  public void reportSuccess(Message message) {
    LOGGER.info("{} successful", name);
  }

  @Override
  public void reportFail(Message message, StopProcessingException e) {
    LOGGER.error(
        "{} failed terminally ({}, {})",
        name,
        keyValue("message", message),
        keyValue("exception", e.toString()),
        e);
  }

  @Override
  public void reportFailAfterMaxRetries(Message message, Exception e) {
    LOGGER.error(
        "{} failed after maximum number of retries ({}, {})",
        name,
        keyValue("message", message),
        keyValue("exception", e.toString()),
        e);
  }

  @Override
  public void reportReject(Message message, Exception e) {
    LOGGER.warn(
        "{}} rejected for retry ({}, {})",
        name,
        keyValue("message", message),
        keyValue("exception", e.toString()));
  }
}
