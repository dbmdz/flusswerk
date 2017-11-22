package de.digitalcollections.workflow.engine;

import de.digitalcollections.workflow.engine.model.Message;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class FlowBuilder<R, W> {

  private String inputChannel;

  private Function<Message, R> reader;

  private Function<R, W> transformer;

  private String outputChannel;

  private Function<W, Message> writer;

  @SuppressWarnings("unchecked")
  private W cast(R value) {
    return (W) value;
  }

  public FlowBuilder<R, W> read(String routingKey, Function<Message, R> reader) {
    if (routingKey  == null || routingKey.isEmpty()) {
      throw new IllegalArgumentException("The routingKey cannot be null or empty.");
    }
    if (reader == null) {
      throw new IllegalArgumentException("The reader cannot be null.");
    }
    this.inputChannel = routingKey;
    this.reader = reader;
    return this;
  }

  public FlowBuilder<R, W> transform(Function<R, W> transformer) {
    if (reader == null) {
      throw new IllegalStateException("You can't transform anything without reading it first. Please add a reader before adding and transformer.");
    }
    if (transformer == null) {
      throw new IllegalArgumentException("The transformer cannot be null.");
    }
    this.transformer = transformer;
    return this;
  }

  public FlowBuilder<R, W> write(String routingKey, Function<W, Message> writer) {
    if (reader != null && transformer == null) {
      this.transformer = this::cast;
    }
    this.writer = requireNonNull(writer);
    this.outputChannel = routingKey;
    return this;
  }

  public Flow<R, W> build() {
    return new Flow<>(inputChannel, outputChannel, reader, transformer, writer);
  }

}
