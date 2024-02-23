package com.github.dbmdz.flusswerk.framework.reporting;

import static java.util.Objects.requireNonNull;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

import com.github.dbmdz.flusswerk.framework.exceptions.RetryProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.model.Envelope;
import com.github.dbmdz.flusswerk.framework.model.Message;

public class DefaultProcessReport implements ProcessReport {

  private final FlusswerkLogger logger;
  private final String name;

  public DefaultProcessReport(String name, Tracing tracing) {
    this.name = requireNonNull(name);
    FlusswerkLoggerFactory loggerFactory = new FlusswerkLoggerFactory(requireNonNull(tracing));
    this.logger = loggerFactory.getFlusswerkLogger(DefaultProcessReport.class);
  }

  public DefaultProcessReport(String name, FlusswerkLogger logger) {
    this.name = requireNonNull(name);
    this.logger = requireNonNull(logger);
  }

  @Override
  public void reportSuccess(Message message) {
    getLogger().info("{} successful", name);
  }

  @Override
  public void reportFail(Message message, StopProcessingException e) {
    Envelope envelope = message.getEnvelope();
    getLogger()
        .error(
            "{} failed terminally: {}",
            name,
            e.getMessage(),
            keyValue("will_retry", false),
            keyValue("incoming_queue", envelope.getSource()),
            keyValue("retries", envelope.getRetries()),
            e);
  }

  @Override
  public void reportFailAfterMaxRetries(Message message, Exception e) {
    Envelope envelope = message.getEnvelope();
    getLogger()
        .error(
            "{} failed after maximum number of retries: {}",
            name,
            e.getMessage(),
            keyValue("will_retry", false),
            keyValue("incoming_queue", envelope.getSource()),
            keyValue("retries", envelope.getRetries()),
            e);
  }

  @Override
  public void reportReject(Message message, Exception e) {
    /*
     * Framework should not call this method anymore since there are more specific methods
     */
    throw new RuntimeException("Not implemented", e);
  }

  @Override
  public void reportRetry(Message message, RuntimeException e) {
    Envelope envelope = message.getEnvelope();
    getLogger()
        .warn(
            "{} rejected for retry: {}",
            name,
            e.getMessage(),
            keyValue("will_retry", true),
            keyValue("incoming_queue", envelope.getSource()),
            keyValue("retries", envelope.getRetries()),
            e);
  }

  @Override
  public void reportComplexRetry(Message message, RetryProcessingException e) {
    int newMessagesToRetry = e.getMessagesToRetry().size();
    int messagesSent = e.getMessagesToSend().size();
    Envelope envelope = message.getEnvelope();
    getLogger()
        .warn(
            "{} rejected for retry with ({} new, {} sent) and : {}",
            name,
            newMessagesToRetry,
            messagesSent,
            e.getMessage(),
            keyValue("will_retry", true),
            keyValue("incoming_queue", envelope.getSource()),
            keyValue("retries", envelope.getRetries()),
            e);
  }

  @Override
  public void reportSkip(Message message, Exception skip) {
    getLogger().info("Skipped: {}", skip.getMessage());
  }

  protected FlusswerkLogger getLogger() {
    return logger;
  }
}
