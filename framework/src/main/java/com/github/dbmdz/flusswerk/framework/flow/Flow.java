package com.github.dbmdz.flusswerk.framework.flow;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import com.github.dbmdz.flusswerk.framework.locking.LockManager;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.monitoring.FlowMetrics;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.MDC;

/**
 * Recipe for the data processing. Every message will be processed by the readerFactory, then the
 * transformerFactory and finally the writerFactory. The transformerFactory can be omitted if <code>
 * R</code> and <code>W</code> are the same.
 */
public class Flow {

  private final Function<Message, Object> reader;
  private final Function<Object, Object> transformer;
  private final Function<Object, Collection<Message>> writer;
  private final Runnable cleanup;
  private final Set<Consumer<FlowInfo>> flowMetrics;
  private final LockManager lockManager;
  private final Tracing tracing;

  public Flow(FlowSpec flowSpec, LockManager lockManager, Tracing tracing) {
    this.reader = requireNonNull(flowSpec.getReader());
    this.transformer = requireNonNull(flowSpec.getTransformer());
    this.writer = requireNonNull(flowSpec.getWriter());
    this.cleanup = requireNonNullElse(flowSpec.getCleanup(), () -> {});
    this.flowMetrics = new HashSet<>();
    if (flowSpec.getMonitor() != null) {
      this.flowMetrics.add(flowSpec.getMonitor());
    }
    this.lockManager = lockManager;
    this.tracing = requireNonNull(tracing);
  }

  public void registerFlowMetrics(Set<FlowMetrics> flowMetrics) {
    this.flowMetrics.addAll(flowMetrics);
  }

  public Collection<Message> process(Message message) {
    FlowInfo info = new FlowInfo(message);
    setLoggingData(message);

    Collection<Message> result;
    long start = System.nanoTime();
    try {
      result = innerProcess(message);
    } catch (RuntimeException e) {
      info.setStatusFrom(e);
      throw e; // Throw exception again after inspecting for ensure control flow in engine
    } finally {
      info.stop();
      long stop = System.nanoTime();
      double duration = (stop - start) / 1e6;
      MDC.put("duration_ms", Double.toString(duration));
      flowMetrics.forEach(
          metric -> metric.accept(info)); // record metrics only available from inside the framework
      lockManager.release(); // make sure any lock has been released
    }
    return result;
  }

  public Collection<Message> innerProcess(Message message) {
    Collection<Message> result;

    try {
      var r = reader.apply(message);
      var t = transformer.apply(r);
      result = writer.apply(t);
    } finally {
      cleanup.run();
    }
    if (result == null) {
      return Collections.emptyList();
    }

    result.stream()
        .filter(Objects::nonNull)
        .filter(m -> m.getTracing() == null || m.getTracing().isEmpty())
        .forEach(m -> m.setTracing(tracing.tracingPath()));

    return result;
  }

  void setLoggingData(Message message) {
    MDC.clear(); // Remove logging data from previous message
    for (Method method : message.getClass().getMethods()) {
      if (!("getId".equalsIgnoreCase(method.getName()) && method.canAccess(message))) {
        continue;
      }
      try {
        Object id = method.invoke(message);
        if (id != null) {
          MDC.put("id", id.toString());
        }
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException("Cannot get ID for logging but should be able to");
      }
      break; // found the id, no need to search further
    }
  }
}
