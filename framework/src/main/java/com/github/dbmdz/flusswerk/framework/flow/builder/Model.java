package com.github.dbmdz.flusswerk.framework.flow.builder;

import com.github.dbmdz.flusswerk.framework.flow.FlowStatus;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

class Model<M extends Message, R, W> {
  private Function<M, R> reader = null;
  private Function<R, W> transformer = null;
  private Function<W, Collection<Message>> writer = null;
  private Consumer<FlowStatus> metrics = null;
  private Runnable cleanup = null;
  private boolean propagateFlowIds = false;

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

  public Consumer<FlowStatus> getMetrics() {
    return metrics;
  }

  public void setMetrics(Consumer<FlowStatus> monitor) {
    this.metrics = monitor;
  }

  public Runnable getCleanup() {
    return cleanup;
  }

  public void setCleanup(Runnable cleanup) {
    this.cleanup = cleanup;
  }

  public boolean isPropagateFlowIds() {
    return propagateFlowIds;
  }

  public void setPropagateFlowIds(boolean propagateFlowIds) {
    this.propagateFlowIds = propagateFlowIds;
  }
}
