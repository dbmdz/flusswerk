package de.digitalcollections.flusswerk.engine.model;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public class Job<M, R, W> {

  private R dataRead;

  private W dataTransformed;

  private M message;

  private Collection<? extends Message> result;

  public Job(M message) {
    this.message = message;
    this.dataRead = null;
    this.dataTransformed = null;
  }

  public void read(Function<M, R> reader) {
    dataRead = reader.apply(message);
  }

  public void transform(Function<R, W> transformer) {
    dataTransformed = transformer.apply(dataRead);
  }

  public void write(Function<W, Collection<? extends Message>> writer) {
    result = writer.apply(dataTransformed);
  }

  public void write(Consumer<W> writer) {
    writer.accept(dataTransformed);
  }

  public M getMessage() {
    return message;
  }

  public Collection<? extends Message> getResult() {
    return result;
  }

}
