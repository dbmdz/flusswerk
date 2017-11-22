package de.digitalcollections.workflow.engine.model;

import java.util.function.Function;

public class Job<R, W> {

  private final Function<Message, R> reader;

  private final Function<R, W> transformer;

  private final Function<W, Message>  writer;

  private R dataRead;

  private W dataTransformed;

  private Message message;

  private Message result;

  public Job(Message message, Function<Message, R> reader, Function<R, W> transformer, Function<W, Message>  writer) {
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

  public Message getMessage() {
    return message;
  }

  public Message getResult() {
    return result;
  }

}
