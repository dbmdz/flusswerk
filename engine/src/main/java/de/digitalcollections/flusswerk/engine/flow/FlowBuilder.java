package de.digitalcollections.flusswerk.engine.flow;

import de.digitalcollections.flusswerk.engine.Engine;
import de.digitalcollections.flusswerk.engine.model.Message;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Builder to create the {@link Flow} to process the data using an {@link Engine}.
 *
 * @param <R> The data type produced by the reader. Input data type of the transformer.
 * @param <W> The data type consumed by the writer. Output data type of the transformer.
 */
public class FlowBuilder<M extends Message, R, W> {

  private Supplier<Function<M, R>> readerFactory;

  private Supplier<Function<R, W>> transformerFactory;

  private Supplier<Function<W, Collection<Message>>> writerFactory;

  private Supplier<Consumer<W>> consumingWriterFactory;

  @SuppressWarnings("unchecked")
  private W cast(R value) {
    return (W) value;
  }

  /**
   * Sets the reader for this flow. The same reader instance will be used for every message, so be careful to keep those thread save.
   *
   * @param reader The reader to process incoming messages.
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link Flow}.
   */
  public FlowBuilder<M, R, W> read(Function<M, R> reader) {
    requireNonNull(reader, "The reader cannot be null");
    this.readerFactory = () -> reader;
    return this;
  }


  /**
   * Sets a reader factory for this flow which creates a new reader for every processed message.
   *
   * @param readerFactory The reader factory to provide readers for incoming messages.
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link Flow}.
   */
  public FlowBuilder<M, R, W> read(Supplier<Function<M, R>> readerFactory) {
    this.readerFactory = requireNonNull(readerFactory,"The reader factory cannot be null.");
    return this;
  }

  /**
   * Sets the transformer for this flow. The same transformer instance will be used for every message, so be careful to keep those thread save.
   *
   * @param transformer The transformer to process data produced by the reader, sending it further to the writer.
   * @return This {@link FlowBuilder} instance for further configuration of the {@link Flow}.
   */
  public FlowBuilder<M, R, W> transform(Function<R, W> transformer) {
    if (readerFactory == null) {
      throw new IllegalStateException("You can't transform anything without reading it first. Please add a reader before adding a transformer.");
    }
    requireNonNull(transformer, "The transformer cannot be null.");
    this.transformerFactory = () -> transformer;
    return this;
  }

  /**
   * Sets the transformer factory for this flow which creates a new transformer for every processed message.
   *
   * @param transformerFactory The transformer factory to provide transformers.
   * @return This {@link FlowBuilder} instance for further configuration of the {@link Flow}.
   */
  public FlowBuilder<M, R, W> transform(Supplier<Function<R, W>> transformerFactory) {
    if (readerFactory == null) {
      throw new IllegalStateException("You can't transform anything without reading it first. Please add a reader before adding a transformer.");
    }
    this.transformerFactory = requireNonNull(transformerFactory, "The transformer factory cannot be null");
    return this;
  }

  private void createDefaultTransformer() {
    if (readerFactory != null && transformerFactory == null) {
      this.transformerFactory = () -> this::cast;
    }
  }

  /**
   * Sets output queue and writer for this flow.
   *
   * @param writer The writer to produce outgoing messages.
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link Flow}.
   */
  public FlowBuilder<M, R, W> writeAndSend(Function<W, Message> writer) {
    requireNonNull(writer, "The writer cannot be null");
    createDefaultTransformer();
    final WriterAdapter<W> writerAdapter = new WriterAdapter<>(writer);
    this.writerFactory = () -> writerAdapter;
    return this;
  }

  /**
   * Sets writer factory for this flow which creates a new writer for every processed message.
   *
   * @param writerFactory The writer factory to provide a writer for every message.
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link Flow}.
   */
  public FlowBuilder<M, R, W> writeAndSend(Supplier<Function<W, Message>> writerFactory) {
    createDefaultTransformer();
    requireNonNull(writerFactory, "The writer factory cannot be null");
    this.writerFactory = () -> new WriterAdapter<>(writerFactory.get());
    return this;
  }

  /**
   * Sets output queue and writer for this flow.
   *
   * @param writer The writer to produce outgoing messages.
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link Flow}.
   */
  public FlowBuilder<M, R, W> writeAndSendMany(Function<W, Collection<Message>> writer) {
    requireNonNull(writer, "The writer factory cannot be null");
    createDefaultTransformer();
    this.writerFactory = () -> writer;
    return this;
  }

  /**
   * Sets writer factory for this flow which creates a new writer for every processed message.
   *
   * @param writerFactory The writer factory to provide a writer for every message.
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link Flow}.
   */
  public FlowBuilder<M, R, W> writeAndSendMany(Supplier<Function<W, Collection<Message>>> writerFactory) {
    createDefaultTransformer();
    this.writerFactory = requireNonNull(writerFactory, "The writer factory cannot be null");
    return this;
  }

  public FlowBuilder<M, R, W> write(Supplier<Consumer<W>> consumingWriterFactory) {
    createDefaultTransformer();
    this.consumingWriterFactory = requireNonNull(consumingWriterFactory, "The writer factory cannot be null");
    return this;
  }

  public FlowBuilder<M, R, W> write(Consumer<W> consumingWriter) {
    requireNonNull(consumingWriter, "The writer cannot be null");
    createDefaultTransformer();
    this.consumingWriterFactory = () -> consumingWriter;
    return this;
  }

  /**
   * Finally builds the flow.
   *
   * @return A new {@link Flow} as configured before.
   */
  public Flow<M, R, W> build() {
    return new Flow<M, R, W>(readerFactory, transformerFactory, writerFactory, consumingWriterFactory);
  }

  public static <M extends Message, R, W> FlowBuilder<M, R, W> receiving(Class<M> clazz) {
    return new FlowBuilder<>();
  }

}
