package de.digitalcollections.workflow.engine;

import de.digitalcollections.workflow.engine.model.Message;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Builder to create the {@link Flow} to process the data using an {@link Engine}.
 *
 * @param <R> The data type produced by the reader. Input data type of the transformer.
 * @param <W> The data type consumed by the writer. Output data type of the transformer.
 */
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

  /**
   * Sets input queue and reader for this flow.
   *
   * @param routingKey The input queue.
   * @param reader The reader to process incoming messages.
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link Flow}.
   */
  public FlowBuilder<R, W> read(String routingKey, Function<Message, R> reader) {
    if (routingKey == null || routingKey.isEmpty()) {
      throw new IllegalArgumentException("The routingKey cannot be null or empty.");
    }
    if (reader == null) {
      throw new IllegalArgumentException("The reader cannot be null.");
    }
    this.inputChannel = routingKey;
    this.reader = reader;
    return this;
  }

  /**
   * Sets the transformer for this flow.
   *
   * @param transformer The transformer to process data produced by the reader, sending it further to the writer.
   * @return This {@link FlowBuilder} instance for further configuration of the {@link Flow}.
   */
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

  /**
   * Sets output queue and writer for this flow.
   *
   * @param routingKey The output queue.
   * @param writer The writer to process incoming messages.
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link Flow}.
   */
  public FlowBuilder<R, W> write(String routingKey, Function<W, Message> writer) {
    if (reader != null && transformer == null) {
      this.transformer = this::cast;
    }
    this.writer = requireNonNull(writer);
    this.outputChannel = routingKey;
    return this;
  }

  /**
   * Finally builds the flow.
   *
   * @return A new {@link Flow} as configured before.
   */
  public Flow<R, W> build() {
    return new Flow<>(inputChannel, outputChannel, reader, transformer, writer);
  }

}
