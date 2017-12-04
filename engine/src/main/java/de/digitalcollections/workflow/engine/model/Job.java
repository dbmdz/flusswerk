package de.digitalcollections.workflow.engine.model;

import java.util.function.Function;

public class Job<M, R, W> {

  private final Function<M, R> reader;

  private final Function<R, W> transformer;

  private final Function<W, Message>  writer;

  private R dataRead;

  private W dataTransformed;

  private M message;

  private Message result;

  public Job(M message, Function<M, R> reader, Function<R, W> transformer, Function<W, Message>  writer) {
    this.message = message;
    this.reader = reader;
    this.transformer = transformer;
    this.writer = writer;
    this.dataRead = null;
    this.dataTransformed = null;
  }

  public void read() {
    dataRead = reader.apply(message);
  }

  public void transform() {
    dataTransformed = transformer.apply(dataRead);
  }

  public void write() {
    result = writer.apply(dataTransformed);
  }

  public M getMessage() {
    return message;
  }

  public Message getResult() {
    return result;
  }

}
