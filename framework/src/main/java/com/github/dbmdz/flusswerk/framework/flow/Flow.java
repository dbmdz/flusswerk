package com.github.dbmdz.flusswerk.framework.flow;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Recipe for the data processing. Every message will be processed by the readerFactory, then the
 * transformerFactory and finally the writerFactory. The transformerFactory can be omitted if <code>
 * R</code> and <code>W</code> are the same.
 *
 * @param <M> The data type of the message.
 * @param <R> The data type produced by the reader. Input data type of the transformer.
 * @param <W> The data type consumed by the writer. Output data type of the transformer.
 */
public class Flow<M extends Message, R, W> {

  private final Function<M, R> reader;

  private final Function<R, W> transformer;

  private final Function<W, Collection<Message>> writer;

  private final Runnable cleanup;

  private final Consumer<FlowMetrics> monitor;

  public Flow(
      Function<M, R> reader,
      Function<R, W> transformer,
      Function<W, Collection<Message>> writer,
      Runnable cleanup,
      Consumer<FlowMetrics> monitor) {
    this.reader = requireNonNull(reader);
    this.transformer = requireNonNull(transformer);
    this.writer = requireNonNull(writer);
    this.cleanup = requireNonNullElse(cleanup, () -> {});
    this.monitor = requireNonNullElse(monitor, metrics -> {});
  }

  public Collection<Message> process(M message) {
    FlowMetrics metrics = new FlowMetrics();
    Collection<Message> result;

    try {
      var r = reader.apply(message);
      var t = transformer.apply(r);
      result = writer.apply(t);
    } catch (RuntimeException e) {
      metrics.setStatusFrom(e);
      throw e; // Throw exception again after inspecting for ensure control flow in engine
    } finally {
      cleanup.run();
      metrics.stop();
      monitor.accept(metrics); // record metrics only available from inside the framework
    }
    if (result == null) {
      return Collections.emptyList();
    }
    for (Message newMessage : result) {
      if (newMessage == null || newMessage.getTracingId() != null) {
        continue; // Do not update the tracing id if the user set one by hand
      }
      newMessage.setTracingId(message.getTracingId());
    }
    return result;
  }

  public boolean hasMessagesToSend() {
    return writer != null;
  }
}
