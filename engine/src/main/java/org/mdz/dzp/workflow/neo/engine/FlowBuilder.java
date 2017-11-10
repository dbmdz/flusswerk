package org.mdz.dzp.workflow.neo.engine;

import java.util.function.Function;
import org.mdz.dzp.workflow.neo.engine.model.Message;

import static java.util.Objects.requireNonNull;

public class FlowBuilder<R, W> {

  private String inputChannel;

  private Function<Message, R> reader;

  private Function<R, W> transformer;

  private String outputChannel;

  private Function<W, Message> writer;

  private String exchange;

  private String deadLetterExchange;

  @SuppressWarnings("unchecked")
  private W cast(R value) {
    return (W) value;
  }

  public FlowBuilder<R, W> read(String routingKey, Function<Message, R> reader) {
    this.inputChannel = requireNonNull(routingKey);
    this.reader = requireNonNull(reader);
    return this;
  }

  public FlowBuilder<R, W> transform(Function<R, W> transformer) {
    if (reader == null) {
      throw new IllegalStateException("You can't transform anything without reading it first. Please add a reader before adding and transformer.");
    }
    this.transformer = requireNonNull(transformer);
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

  public FlowBuilder<R, W> exchange(String name) {
    this.exchange = name;
    return this;
  }

  public FlowBuilder<R, W> deadLetterExchange(String name) {
    this.deadLetterExchange = name;
    return this;
  }

  public Flow<R, W> build() {
    return new Flow<>(inputChannel, outputChannel, reader, transformer, writer, exchange, deadLetterExchange);
  }

}
