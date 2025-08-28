package dev.mdz.flusswerk.reporting;

import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import net.logstash.logback.argument.StructuredArgument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

@DisplayName("The FlusswerkLogger")
class FlusswerkLoggerTest {

  private Logger logger;
  private FlusswerkLogger flusswerkLogger;
  private Tracing tracing;

  @BeforeEach
  void setUp() {
    logger = mock(Logger.class);
    tracing = mock(Tracing.class);
    flusswerkLogger = new FlusswerkLogger(logger, tracing);
  }

  @DisplayName("should use error logging")
  @Test
  void error() {
    flusswerkLogger.error("format string", "a");
    verify(logger).error(anyString(), any(Object[].class));
  }

  @DisplayName("should move Throwable to the end")
  @Test
  void shouldMoveThrowableToTheEnd() {
    var throwable = mock(Throwable.class);
    flusswerkLogger.error("format string", "a", throwable);
    verify(logger)
        .error(eq("format string"), eq("a"), any(StructuredArgument.class), eq(throwable));
  }

  @DisplayName("should use warn logging")
  @Test
  void warn() {
    flusswerkLogger.warn("format string", "a");
    verify(logger).warn(anyString(), any(Object[].class));
  }

  @DisplayName("should use info logging")
  @Test
  void info() {
    flusswerkLogger.info("format string", "a");
    verify(logger).info(anyString(), any(Object[].class));
  }

  @DisplayName("should use debug logging")
  @Test
  void debug() {
    flusswerkLogger.debug("format string", "a");
    verify(logger).debug(anyString(), any(Object[].class));
  }

  @DisplayName("should use trace logging")
  @Test
  void trace() {
    flusswerkLogger.trace("format string", "a");
    verify(logger).trace(anyString(), any(Object[].class));
  }

  @DisplayName("should add tracing information")
  @Test
  void addTracing() {
    when(tracing.tracingPath()).thenReturn(List.of("TRACING"));
    Object[] a = {"A"};
    Object[] actual = flusswerkLogger.addTracing(a);
    assertThat(actual).containsExactly("A", keyValue("tracing", tracing.tracingPath()));
  }
}
