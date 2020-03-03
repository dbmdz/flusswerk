package de.digitalcollections.flusswerk.engine.flow;

import de.digitalcollections.flusswerk.engine.exceptions.StopProcessingException;
import de.digitalcollections.flusswerk.engine.flow.FlowStatus.Status;
import de.digitalcollections.flusswerk.engine.model.Job;
import de.digitalcollections.flusswerk.engine.model.Message;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOGGER = LoggerFactory.getLogger(Flow.class);

  private final Supplier<Function<M, R>> readerFactory;

  private final Supplier<Function<R, W>> transformerFactory;

  private final Supplier<Function<W, Collection<Message>>> writerFactory;

  private final Supplier<Consumer<W>> consumingWriterFactory;

  private final Runnable cleanup;

  private final boolean propagateFlowIds;

  private final Consumer<FlowStatus> monitor;

  public Flow(
      Supplier<Function<M, R>> readerFactory,
      Supplier<Function<R, W>> transformerFactory,
      Supplier<Function<W, Collection<Message>>> writerFactory,
      Supplier<Consumer<W>> consumingWriterFactory,
      Runnable cleanup,
      boolean propagateFlowIds,
      Consumer<FlowStatus> monitor) {
    this.readerFactory = readerFactory;
    this.transformerFactory = transformerFactory;
    this.writerFactory = writerFactory;
    this.consumingWriterFactory = consumingWriterFactory;
    this.cleanup = cleanup;
    this.propagateFlowIds = propagateFlowIds;
    this.monitor = monitor;
  }

  public Collection<Message> process(M message) {
    FlowStatus flowStatus = new FlowStatus();
    Job<M, R, W> job = new Job<>(message, propagateFlowIds);

    Collection<Message> result = null;

    try {
      if (readerFactory != null) {
        job.read(readerFactory.get());
      }
      if (transformerFactory != null) {
        job.transform(transformerFactory.get());
      }
      if (writerFactory != null) {
        job.write(writerFactory.get());
      }
      if (consumingWriterFactory != null) {
        job.write(consumingWriterFactory.get());
      }

    } catch (RuntimeException e) {
      if (e instanceof StopProcessingException) {
        flowStatus.setStatus(Status.ERROR_STOP);
      } else {
        flowStatus.setStatus(Status.ERROR_RETRY);
      }
      throw e; // Throw exception again after inspecting for ensure control flow in engine
    } finally {
      result = job.getResult();

      // If in the cleanup stage, a garbage collection is forced,
      // then it helps to clean up before.
      job = null;

      if (cleanup != null) {
        cleanup.run();
      }
      flowStatus.stop();
      if (monitor != null) {
        monitor.accept(flowStatus);
      }
    }

    return result;
  }

  public boolean hasMessagesToSend() {
    return writerFactory != null;
  }
}
