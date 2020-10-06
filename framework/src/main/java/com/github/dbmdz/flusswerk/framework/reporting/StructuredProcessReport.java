package com.github.dbmdz.flusswerk.framework.reporting;

import static java.util.Objects.requireNonNull;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.model.Envelope;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StructuredProcessReport implements ProcessReport {

  private static final Logger LOGGER = LoggerFactory.getLogger(StructuredProcessReport.class);

  private final String name;
  private final Tracing tracing;

  public StructuredProcessReport(String name, Tracing tracing) {
    this.name = requireNonNull(name);
    this.tracing = requireNonNull(tracing);
  }

  @Override
  public void reportSuccess(Message message) {
    // should be default, so no need to log anything and spam the logs
  }

  @Override
  public void reportFail(Message message, StopProcessingException e) {
    Envelope envelope = message.getEnvelope();
    getLogger()
        .error(
            "{} failed, will not retry",
            name,
            keyValue("will_retry", false),
            keyValue("incoming_queue", envelope.getSource()),
            keyValue("retries", envelope.getRetries()),
            keyValue("timestamp", envelope.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME)),
            keyValue("tracing_id", message.getTracingId()),
            keyValue("tracing", tracing.tracingPath()),
            e);
  }

  @Override
  public void reportFailAfterMaxRetries(Message message, Exception e) {
    Envelope envelope = message.getEnvelope();
    getLogger()
        .error(
            "{} failed after max retries",
            name,
            keyValue("will_retry", false),
            keyValue("incoming_queue", envelope.getSource()),
            keyValue("retries", envelope.getRetries()),
            keyValue("timestamp", envelope.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME)),
            keyValue("tracing_id", message.getTracingId()),
            keyValue("tracing", tracing.tracingPath()),
            e);
  }

  @Override
  public void reportReject(Message message, Exception e) {
    Envelope envelope = message.getEnvelope();
    getLogger()
        .error(
            "{} failed, but will retry later",
            name,
            keyValue("will_retry", true),
            keyValue("incoming_queue", envelope.getSource()),
            keyValue("retries", envelope.getRetries()),
            keyValue("timestamp", envelope.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME)),
            keyValue("tracing_id", message.getTracingId()),
            keyValue("tracing", tracing.tracingPath()),
            e);
  }

  protected Logger getLogger() {
    return LOGGER;
  }
}
