package com.github.dbmdz.flusswerk.framework.flow;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.model.HasFlowId;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builder to create the {@link Flow} to process the data using an {@link Engine}.
 *
 * @param <M> message
 * @param <R> The data type produced by the reader. Input data type of the transformer.
 * @param <W> The data type consumed by the writer. Output data type of the transformer.
 */
public class FlowBuilder<M extends Message, R, W> {

  private Supplier<Function<M, R>> readerFactory;

  private Supplier<Function<R, W>> transformerFactory;

  private Supplier<Function<W, Collection<Message>>> writerFactory;

  private Supplier<Consumer<W>> consumingWriterFactory;

  private Runnable cleanup;

  private boolean propagateFlowIds;

  private Consumer<FlowStatus> monitor;

  public FlowBuilder() {
    this.propagateFlowIds = false;
  }

  @SuppressWarnings("unchecked")
  private W cast(R value) {
    return (W) value;
  }

  /**
   * Sets the reader for this flow. The same reader instance will be used for every message, so be
   * careful to keep those thread save.
   *
   * @param reader The reader to process incoming messages.
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link
   *     Flow}.
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
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link
   *     Flow}.
   */
  public FlowBuilder<M, R, W> read(Supplier<Function<M, R>> readerFactory) {
    this.readerFactory = requireNonNull(readerFactory, "The reader factory cannot be null.");
    return this;
  }

  /**
   * Sets the transformer for this flow. The same transformer instance will be used for every
   * message, so be careful to keep those thread save.
   *
   * @param transformer The transformer to process data produced by the reader, sending it further
   *     to the writer.
   * @return This {@link FlowBuilder} instance for further configuration of the {@link Flow}.
   */
  public FlowBuilder<M, R, W> transform(Function<R, W> transformer) {
    if (readerFactory == null) {
      throw new IllegalStateException(
          "You can't transform anything without reading it first. Please add a reader before adding a transformer.");
    }
    requireNonNull(transformer, "The transformer cannot be null.");
    this.transformerFactory = () -> transformer;
    return this;
  }

  /**
   * Sets the transformer factory for this flow which creates a new transformer for every processed
   * message.
   *
   * @param transformerFactory The transformer factory to provide transformers.
   * @return This {@link FlowBuilder} instance for further configuration of the {@link Flow}.
   */
  public FlowBuilder<M, R, W> transform(Supplier<Function<R, W>> transformerFactory) {
    if (readerFactory == null) {
      throw new IllegalStateException(
          "You can't transform anything without reading it first. Please add a reader before adding a transformer.");
    }
    this.transformerFactory =
        requireNonNull(transformerFactory, "The transformer factory cannot be null");
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
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link
   *     Flow}.
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
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link
   *     Flow}.
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
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link
   *     Flow}.
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
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link
   *     Flow}.
   */
  public FlowBuilder<M, R, W> writeAndSendMany(
      Supplier<Function<W, Collection<Message>>> writerFactory) {
    createDefaultTransformer();
    this.writerFactory = requireNonNull(writerFactory, "The writer factory cannot be null");
    return this;
  }

  public FlowBuilder<M, R, W> write(Supplier<Consumer<W>> consumingWriterFactory) {
    createDefaultTransformer();
    this.consumingWriterFactory =
        requireNonNull(consumingWriterFactory, "The writer factory cannot be null");
    return this;
  }

  public FlowBuilder<M, R, W> write(Consumer<W> consumingWriter) {
    requireNonNull(consumingWriter, "The writer cannot be null");
    createDefaultTransformer();
    this.consumingWriterFactory = () -> consumingWriter;
    return this;
  }

  /**
   * Copy flowIds from incoming messages to all outgoing messages. Requires both to implement {@link
   * HasFlowId}.
   *
   * @param propagateFlowIds true to propagate flow ids (default: <code>false</code>)
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link
   *     Flow}.
   */
  public FlowBuilder<M, R, W> propagateFlowIds(boolean propagateFlowIds) {
    this.propagateFlowIds = propagateFlowIds;
    return this;
  }

  /**
   * Sets cleanup runnable, which is executed after the message was processed.
   *
   * @param runnable The runnable to be executed after the message was processed
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link
   *     Flow}.
   */
  public FlowBuilder<M, R, W> cleanup(Runnable runnable) {
    requireNonNull(runnable, "The runnable cannot be null");
    this.cleanup = runnable;
    return this;
  }

  /**
   * Sets a monitoring callback that is called after all processing and cleanup is finished. Metrics
   * provided by flow status are provided by Flusswerk and would be inaccessible otherwise.
   *
   * @param consumer The consumer to be executed to record status.
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link
   *     Flow}.
   * @deprecated use <code>measure</code> instead
   */
  @Deprecated
  public FlowBuilder<M, R, W> monitor(Consumer<FlowStatus> consumer) {
    requireNonNull(consumer, "The runnable cannot be null");
    this.monitor = consumer;
    return this;
  }

  /**
   * Sets a callback that is called after all processing and cleanup is finished to collect data on
   * flow execution. Metrics provided by flow status are provided by Flusswerk and would be
   * inaccessible otherwise.
   *
   * @param consumer The consumer to be executed to record status.
   * @return This {@link FlowBuilder} instance for further configuration or creation of the {@link
   *     Flow}.
   */
  public FlowBuilder<M, R, W> measure(Consumer<FlowStatus> consumer) {
    requireNonNull(consumer, "The runnable cannot be null");
    this.monitor = consumer;
    return this;
  }

  /**
   * Finally builds the flow.
   *
   * @return A new {@link Flow} as configured before.
   */
  public Flow<M, R, W> build() {
    return new Flow<>(
        readerFactory,
        transformerFactory,
        writerFactory,
        consumingWriterFactory,
        cleanup,
        propagateFlowIds,
        monitor);
  }

  public static <M extends Message, R, W> FlowBuilder<M, R, W> receiving(Class<M> clazz) {
    return new FlowBuilder<>();
  }

  public static <M extends Message, R, W> FlowBuilder<M, R, W> with(
      Class<M> messageClass, Class<R> transformerInput, Class<W> transformerOutput) {
    return new FlowBuilder<>();
  }

  /**
   * Create a FlowBuilder for the specified types. The use of {@link Type} instances allows to
   * declare types with specific generics (e.g. <code>Type&lt;List&lt;String&gt;&gt;</code>).
   *
   * @param message The message type
   * @param transformerInput The type for the transformer input (=reader output)
   * @param transformerOutput The type for the transformer output (=writer input)
   * @param <M> The message type
   * @param <R> The type for the transformer input (=reader output).
   * @param <W> The type for the transformer output (=writer input)
   * @return a new {@link FlowBuilder} instance.
   */
  public static <M extends Message, R, W> FlowBuilder<M, R, W> with(
      Type<M> message, Type<R> transformerInput, Type<W> transformerOutput) {
    return new FlowBuilder<>();
  }

  /**
   * Create a FlowBuilder for the specified types.
   *
   * <p>This is a convenience API for flows that use the same input datatype for transformer and
   * writer
   *
   * @param messageClass The message class
   * @param dataClass The data class
   * @param <M> The message type
   * @param <T> The data type
   * @return a new {@link FlowBuilder} instance
   */
  public static <M extends Message, T> FlowBuilder<M, T, T> with(
      Class<M> messageClass, Class<T> dataClass) {
    return new FlowBuilder<>();
  }

  /**
   * Create a FlowBuilder for the specified types. The use of {@link Type} instances allows to
   * declare types with specific generics (e.g. <code>Type&lt;List&lt;String&gt;&gt;</code>).
   *
   * <p>This is a convenience API for flows that use the same input datatype for transformer and
   * writer
   *
   * @param message The message type
   * @param data The data type
   * @param <M> The message type
   * @param <T> The data type
   * @return a new {@link FlowBuilder} instance
   */
  public static <M extends Message, T> FlowBuilder<M, T, T> with(Type<M> message, Type<T> data) {
    return new FlowBuilder<>();
  }

  public static <M extends Message<?>, R, W> Flow<M, R, W> createFlow(
      Class<M> cls, Function<M, R> reader, Function<R, W> transformer, Consumer<W> writer) {
    return FlowBuilder.<M, R, W>receiving(cls)
        .read(reader)
        .transform(transformer)
        .write(writer)
        .build();
  }

  public static <M extends Message<?>, R, W> Flow<M, R, W> createFlow(
      Class<M> cls, Function<M, R> reader, Consumer<W> writer) {
    return FlowBuilder.<M, R, W>receiving(cls).read(reader).write(writer).build();
  }

  public static <M extends Message<?>, R, W> Flow<M, R, W> createFlow(
      Class<M> cls, Function<M, R> reader) {
    return FlowBuilder.<M, R, W>receiving(cls).read(reader).build();
  }
}
