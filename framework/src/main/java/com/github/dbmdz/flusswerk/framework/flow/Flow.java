package com.github.dbmdz.flusswerk.framework.flow;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import com.github.dbmdz.flusswerk.framework.locking.LockManager;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.monitoring.FlowMetrics;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
  private final Set<FlowMetrics> flowMetrics;
  private final LockManager lockManager;

  public Flow(FlowSpec<M, R, W> flowSpec, LockManager lockManager) {
    this.reader = requireNonNull(flowSpec.getReader());
    this.transformer = requireNonNull(flowSpec.getTransformer());
    this.writer = requireNonNull(flowSpec.getWriter());
    this.cleanup = requireNonNullElse(flowSpec.getCleanup(), () -> {});
    this.flowMetrics = new HashSet<>();
    this.lockManager = lockManager;
  }

  public void registerFlowMetrics(Set<FlowMetrics> flowMetrics) {
    this.flowMetrics.addAll(flowMetrics);
  }

  public Collection<Message> process(M message) {
    FlowInfo info = new FlowInfo();
    Collection<Message> result;

    try {
      var r = reader.apply(message);
      var t = transformer.apply(r);
      result = writer.apply(t);
    } catch (RuntimeException e) {
      info.setStatusFrom(e);
      throw e; // Throw exception again after inspecting for ensure control flow in engine
    } finally {
      cleanup.run();
      info.stop();
      flowMetrics.forEach(metric -> metric.accept(info)); // record metrics only available from inside the framework
      lockManager.release(); // make sure any lock has been released
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
}
