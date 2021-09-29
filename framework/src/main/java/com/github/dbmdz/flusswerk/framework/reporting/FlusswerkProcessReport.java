package com.github.dbmdz.flusswerk.framework.reporting;

import static java.util.Objects.requireNonNull;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.model.Envelope;
import com.github.dbmdz.flusswerk.framework.model.Message;

public class FlusswerkProcessReport implements ProcessReport {

  protected final FlusswerkLogger logger;
  protected final String name;

  public FlusswerkProcessReport(String name, FlusswerkLogger logger) {
    this.name = requireNonNull(name);
    this.logger = requireNonNull(logger);
  }

  @Override
  public void reportSuccess(Message message) {
    // should be default, so no need to log anything and spam the logs
  }

  @Override
  public void reportFail(Message message, StopProcessingException e) {
    Envelope envelope = message.getEnvelope();
    logger.error(
        "{} failed, will not retry",
        name,
        keyValue("will_retry", false),
        keyValue("incoming_queue", envelope.getSource()),
        keyValue("retries", envelope.getRetries()),
        e);
  }

  @Override
  public void reportFailAfterMaxRetries(Message message, Exception e) {
    Envelope envelope = message.getEnvelope();
    logger.error(
        "{} failed after max retries",
        name,
        keyValue("will_retry", false),
        keyValue("incoming_queue", envelope.getSource()),
        keyValue("retries", envelope.getRetries()),
        e);
  }

  @Override
  public void reportReject(Message message, Exception e) {
    Envelope envelope = message.getEnvelope();
    logger.error(
        "{} failed, but will retry later",
        name,
        keyValue("will_retry", true),
        keyValue("incoming_queue", envelope.getSource()),
        keyValue("retries", envelope.getRetries()),
        e);
  }
}
