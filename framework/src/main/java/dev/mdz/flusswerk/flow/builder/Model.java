package dev.mdz.flusswerk.flow.builder;

import dev.mdz.flusswerk.flow.FlowInfo;
import dev.mdz.flusswerk.model.Message;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

class Model<M extends Message, R, W> {
  private Function<M, R> reader = null;
  private Function<R, W> transformer = null;
  private Function<W, Collection<Message>> writer = null;
  private Consumer<FlowInfo> metrics = null;
  private Runnable cleanup = null;

  public Function<M, R> getReader() {
    return reader;
  }

  public void setReader(Function<M, R> reader) {
    this.reader = reader;
  }

  public Function<R, W> getTransformer() {
    return transformer;
  }

  public void setTransformer(Function<R, W> transformer) {
    this.transformer = transformer;
  }

  public Function<W, Collection<Message>> getWriter() {
    return writer;
  }

  public void setWriter(Function<W, Collection<Message>> writer) {
    this.writer = writer;
  }

  public Consumer<FlowInfo> getMetrics() {
    return metrics;
  }

  public void setMetrics(Consumer<FlowInfo> monitor) {
    this.metrics = monitor;
  }

  public Runnable getCleanup() {
    return cleanup;
  }

  public void setCleanup(Runnable cleanup) {
    this.cleanup = cleanup;
  }
}
