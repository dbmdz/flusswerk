package com.github.dbmdz.flusswerk.framework.flow;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public class FlowSpec<M, R, W> {

  private final Function<M, R> reader;

  private final Function<R, W> transformer;

  private final Function<W, Collection<Message>> writer;

  private final Runnable cleanup;

  private final Consumer<FlowInfo> monitor;

  public FlowSpec(
      Function<M, R> reader,
      Function<R, W> transformer,
      Function<W, Collection<Message>> writer,
      Runnable cleanup,
      Consumer<FlowInfo> monitor) {
    this.reader = requireNonNull(reader);
    this.transformer = requireNonNull(transformer);
    this.writer = requireNonNull(writer);
    this.cleanup = requireNonNullElse(cleanup, () -> {});
    this.monitor = requireNonNullElse(monitor, metrics -> {});
  }

  public Function<M, R> getReader() {
    return reader;
  }

  public Function<R, W> getTransformer() {
    return transformer;
  }

  public Function<W, Collection<Message>> getWriter() {
    return writer;
  }

  public Runnable getCleanup() {
    return cleanup;
  }

  public Consumer<FlowInfo> getMonitor() {
    return monitor;
  }
}
