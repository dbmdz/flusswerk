package com.github.dbmdz.flusswerk.framework.flow.builder;

import com.github.dbmdz.flusswerk.framework.flow.Type;
import com.github.dbmdz.flusswerk.framework.model.Message;

/**
 * Experimental implementation of a new FlowBuilderApi. This might change in details in Flusswerk 4
 * where it will become the regular implementation.
 */
public class FlowBuilder {

  /**
   * Create a builder for a new read/transform/write flow. Transform might be omitted later if types
   * for reader output (<code>R</code>) and writer input (<code>W</code>) match.
   *
   * @param messageClass the message class
   * @param readerOut the class of reader output/transformer input
   * @param writerIn the class of transformer output/writer input
   * @param <M> Generic type for the message class
   * @param <R> Generic type for the reader output/transformer input
   * @param <W> Generic type for the transformer output/writer input
   * @return a new builder for a new flow
   */
  public static <M extends Message, R, W> SetReaderStep<M, R, W> flow(
      Class<M> messageClass, Class<R> readerOut, Class<W> writerIn) {
    return new SetReaderStep<>(new Model<>());
  }

  /**
   * Create a builder for a new read/transform/write flow for the special case that the reader
   * output type is used throughout the flow. Transform might be omitted later.
   *
   * @param messageClass the message class
   * @param modelType the class of reader output/writer input
   * @param <M> Generic type for the message class
   * @param <T> Generic type for the reader output/writer input
   * @return a new builder for a new flow
   */
  public static <M extends Message, T> SetReaderStep<M, T, T> flow(
      Class<M> messageClass, Class<T> modelType) {
    return new SetReaderStep<>(new Model<>());
  }

  /**
   * Create a builder for a new read/transform/write flow. Transform might be omitted later if types
   * for reader output (<code>R</code>) and writer input (<code>W</code>) match.
   *
   * @param messageClass the message class
   * @param readerOut the type of reader output/transformer input
   * @param writerIn the type of transformer output/writer input
   * @param <M> Generic type for the message class
   * @param <R> Generic type for the reader output/transformer input
   * @param <W> Generic type for the transformer output/writer input
   * @return a new builder for a new flow
   */
  public static <M extends Message, R, W> SetReaderStep<M, R, W> flow(
      Type<M> messageClass, Type<R> readerOut, Type<W> writerIn) {
    return new SetReaderStep<>(new Model<>());
  }

  /**
   * Create a builder for a new read/transform/write flow for the special case that the reader
   * output type is used throughout the flow. Transform might be omitted later.
   *
   * @param messageClass the message type
   * @param modelType the class of reader output/writer input
   * @param <M> Generic type for the message class
   * @param <T> Generic type for the reader output/writer input
   * @return a new builder for a new flow
   */
  public static <M extends Message, T> SetReaderStep<M, T, T> flow(
      Type<M> messageClass, Type<T> modelType) {
    return new SetReaderStep<>(new Model<>());
  }

  /**
   * Create builder for a special flow that operates on the messages only and does not fit the usual
   * read/transform/write pattern.
   *
   * @param messageClass The message class to operate on
   * @param <M> The generic type for the message class to operate on
   * @return a new builder for a new flow
   */
  public static <M extends Message> SetMessageProcessorStep<M> messageProcessor(
      Class<M> messageClass) {
    return new SetMessageProcessorStep<>(new Model<>());
  }

  /**
   * Create builder for a special flow that operates on the messages only and does not fit the usual
   * read/transform/write pattern.
   *
   * @param messageType The message type to operate on
   * @param <M> The generic type for the message class to operate on
   * @return a new builder for a new flow
   */
  public static <M extends Message> SetMessageProcessorStep<M> messageProcessor(
      Type<M> messageType) {
    return new SetMessageProcessorStep<>(new Model<>());
  }
}
