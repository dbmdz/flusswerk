package com.github.dbmdz.flusswerk.framework.reporting;

import static java.util.Objects.requireNonNull;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

import com.github.dbmdz.flusswerk.framework.exceptions.RetryProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultProcessReport implements ProcessReport {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProcessReport.class);

  private final String name;
  private final Tracing tracing;

  public DefaultProcessReport(String name, Tracing tracing) {
    this.name = requireNonNull(name);
    this.tracing = requireNonNull(tracing);
  }

  @Override
  public void reportSuccess(Message message) {
    LOGGER.info("{} successful", name, keyValue("tracing", tracing.tracingPath()));
  }

  @Override
  public void reportFail(Message message, StopProcessingException e) {
    getLogger()
        .error(
            "{} failed terminally: {}",
            name,
            e.getMessage(),
            keyValue("amqp_message", message.toString()),
            e);
  }

  @Override
  public void reportFailAfterMaxRetries(Message message, Exception e) {
    getLogger()
        .error(
            "{} failed after maximum number of retries: {}",
            name,
            e.getMessage(),
            keyValue("amqp_message", message.toString()),
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
    getLogger()
        .warn(
            "{} rejected for retry: {}",
            name,
            e.getMessage(),
            keyValue("amqp_message", message.toString()),
            e);
  }

  @Override
  public void reportComplexRetry(Message message, RetryProcessingException e) {
    int newMessagesToRetry = e.getMessagesToRetry().size();
    int messagesSent = e.getMessagesToSend().size();
    getLogger()
        .warn(
            "{} rejected for retry with ({} new, {} sent) and : {}",
            name,
            newMessagesToRetry,
            messagesSent,
            e.getMessage(),
            keyValue("amqp_message", message.toString()),
            e);
  }

  @Override
  public void reportSkip(Message message, Exception skip) {
    LOGGER.info("Skipped: {}", skip.getMessage());
  }

  protected Logger getLogger() {
    return LOGGER;
  }
}
