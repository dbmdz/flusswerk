package com.github.dbmdz.flusswerk.framework.flow;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public class FlowSpec {

  private final Function<Message, Object> reader;
  private final Function<Object, Object> transformer;
  private final Function<Object, Collection<Message>> writer;

  private final Runnable cleanup;

  private final Consumer<FlowInfo> monitor;

  public FlowSpec(
      Function<Message, Object> reader,
      Function<Object, Object> transformer,
      Function<Object, Collection<Message>> writer,
      Runnable cleanup,
      Consumer<FlowInfo> monitor) {
    this.reader = requireNonNull(reader);
    this.transformer = requireNonNull(transformer);
    this.writer = requireNonNull(writer);
    this.cleanup = requireNonNullElse(cleanup, () -> {});
    this.monitor = requireNonNullElse(monitor, metrics -> {});
  }

  public Function<Message, Object> getReader() {
    return reader;
  }

  public Function<Object, Object> getTransformer() {
    return transformer;
  }

  public Function<Object, Collection<Message>> getWriter() {
    return writer;
  }

  public Runnable getCleanup() {
    return cleanup;
  }

  public Consumer<FlowInfo> getMonitor() {
    return monitor;
  }
}
