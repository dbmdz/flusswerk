package com.github.dbmdz.flusswerk.framework.flow;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import java.util.function.Consumer;
import java.util.function.Function;

public class FlowSpec {

  private final Function reader;

  private final Function transformer;

  private final Function writer;

  private final Runnable cleanup;

  private final Consumer<FlowInfo> monitor;

  public FlowSpec(
      Function reader,
      Function transformer,
      Function writer,
      Runnable cleanup,
      Consumer<FlowInfo> monitor) {
    this.reader = requireNonNull(reader);
    this.transformer = requireNonNull(transformer);
    this.writer = requireNonNull(writer);
    this.cleanup = requireNonNullElse(cleanup, () -> {});
    this.monitor = requireNonNullElse(monitor, metrics -> {});
  }

  public Function getReader() {
    return reader;
  }

  public Function getTransformer() {
    return transformer;
  }

  public Function getWriter() {
    return writer;
  }

  public Runnable getCleanup() {
    return cleanup;
  }

  public Consumer<FlowInfo> getMonitor() {
    return monitor;
  }
}
