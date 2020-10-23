package com.github.dbmdz.flusswerk.framework.reporting;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

import org.slf4j.Logger;

/** Logger class to automatically add tracing paths to your structured logging. */
public class FlusswerkLogger {

  private final Logger logger;
  private final Tracing tracing;

  FlusswerkLogger(Logger logger, Tracing tracing) {
    this.logger = logger;
    this.tracing = tracing;
  }

  /**
   * Log messages with level error.
   *
   * @param format A format string for log messages.
   * @param arguments Arguments for the format string and structured logging.
   */
  public void error(String format, Object... arguments) {
    logger.error(format, addTracing(arguments));
  }

  /**
   * Log messages with level warn.
   *
   * @param format A format string for log messages.
   * @param arguments Arguments for the format string and structured logging.
   */
  public void warn(String format, Object... arguments) {
    logger.warn(format, addTracing(arguments));
  }

  /**
   * Log messages with level info.
   *
   * @param format A format string for log messages.
   * @param arguments Arguments for the format string and structured logging.
   */
  public void info(String format, Object... arguments) {
    logger.info(format, addTracing(arguments));
  }

  /**
   * Log messages with level debug.
   *
   * @param format A format string for log messages.
   * @param arguments Arguments for the format string and structured logging.
   */
  public void debug(String format, Object... arguments) {
    logger.debug(format, addTracing(arguments));
  }

  /**
   * Log messages with level trace.
   *
   * @param format A format string for log messages.
   * @param arguments Arguments for the format string and structured logging.
   */
  public void trace(String format, Object... arguments) {
    logger.trace(format, addTracing(arguments));
  }

  Object[] addTracing(Object[] arguments) {
    Object[] extended = new Object[arguments.length + 1];
    System.arraycopy(arguments, 0, extended, 0, arguments.length);
    extended[extended.length - 1] = keyValue("tracing", tracing.tracingPath());
    return extended;
  }
}
