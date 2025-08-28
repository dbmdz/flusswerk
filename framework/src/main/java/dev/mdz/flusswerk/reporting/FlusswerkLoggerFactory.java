package dev.mdz.flusswerk.reporting;

import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.LoggerFactory;

public class FlusswerkLoggerFactory {

  private final ConcurrentHashMap<Class<?>, FlusswerkLogger> loggers;
  private final Tracing tracing;

  public FlusswerkLoggerFactory(Tracing tracing) {
    this.loggers = new ConcurrentHashMap<>();
    this.tracing = tracing;
  }

  public FlusswerkLogger getFlusswerkLogger(Class<?> cls) {
    return loggers.computeIfAbsent(
        cls, c -> new FlusswerkLogger(LoggerFactory.getLogger(c), tracing));
  }
}
