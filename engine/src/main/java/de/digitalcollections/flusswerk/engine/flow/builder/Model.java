package de.digitalcollections.flusswerk.engine.flow.builder;

import de.digitalcollections.flusswerk.engine.flow.FlowStatus;
import de.digitalcollections.flusswerk.engine.model.Message;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

class Model<M extends Message, R, W> {
  private Function<M, R> reader = null;
  private Function<R, W> transformer = null;
  private Function<W, Collection<Message>> writer = null;
  private Consumer<FlowStatus> monitor = null;
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

  public Consumer<FlowStatus> getMonitor() {
    return monitor;
  }

  public void setMonitor(Consumer<FlowStatus> monitor) {
    this.monitor = monitor;
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
