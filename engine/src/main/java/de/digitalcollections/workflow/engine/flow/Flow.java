package de.digitalcollections.workflow.engine.flow;

import de.digitalcollections.workflow.engine.model.Job;
import de.digitalcollections.workflow.engine.model.Message;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recipe for the data processing. Every message will be processed by the readerFactory, then the transformerFactory and finally the writerFactory. The transformerFactory can be omitted if <code>R</code> and <code>W</code> are the same.
 * @param <R>
 * @param <W>
 */
public class Flow<M extends Message, R, W> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Flow.class);

  private Supplier<Function<M, R>> readerFactory;

  private Supplier<Function<R, W>> transformerFactory;

  private Supplier<Function<W, Message>> writerFactory;

  public Flow(Supplier<Function<M, R>> readerFactory, Supplier<Function<R, W>> transformerFactory, Supplier<Function<W, Message>> writerFactory) {
    this.readerFactory = readerFactory;
    this.transformerFactory = transformerFactory;
    this.writerFactory = writerFactory;
  }
  
  public Message process(M message) {
    Job<M, R, W> job = new Job<>(message);
    if (readerFactory != null) {
      job.read(readerFactory.get());
    }
    if (transformerFactory != null) {
      job.transform(transformerFactory.get());
    }
    if (writerFactory != null) {
      job.write(writerFactory.get());
    }
    return job.getResult();
  }

  public boolean writesData() {
    return writerFactory != null;
  }

}
