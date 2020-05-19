package com.github.dbmdz.flusswerk.framework.model;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

public class Job<M extends Message, R, W> {

  private R dataRead;

  private W dataTransformed;

  private final M message;

  private Collection<Message> result;

  private boolean propagateFlowIds;

  public Job(M message) {
    this.message = message;
    this.dataRead = null;
    this.dataTransformed = null;
  }

  public Job(M message, boolean propagateFlowIds) {
    this(message);
    this.propagateFlowIds = propagateFlowIds;
  }

  public void read(Function<M, R> reader) {
    dataRead = reader.apply(message);
  }

  public void transform(Function<R, W> transformer) {
    dataTransformed = transformer.apply(dataRead);
  }

  public void write(Function<W, Collection<Message>> writer) {
    result = writer.apply(dataTransformed);
  }

  public void write(Consumer<W> writer) {
    writer.accept(dataTransformed);
  }

  public Collection<Message> getResult() {
    if (result == null) {
      return Collections.emptyList();
    }
    if (propagateFlowIds) {
      for (Message newMessage : result) {
        newMessage.setTracingId(message.getTracingId());
      }
    }
    return result;
  }
}