package dev.mdz.flusswerk.fixtures;

import java.util.concurrent.Semaphore;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transformer that blocks the current thread by trying to acquire a drained semaphore.
 *
 * @param <T> The data type to operate on.
 */
class ThreadBlockingTransformer<T> implements UnaryOperator<T> {

  private final Logger LOGGER = LoggerFactory.getLogger(ThreadBlockingTransformer.class);

  private final Semaphore semaphore;

  ThreadBlockingTransformer() {
    semaphore = new Semaphore(1);
    semaphore.drainPermits();
  }

  @Override
  public T apply(T t) {
    long threadId = Thread.currentThread().getId();
    try {
      semaphore.acquire(); // Block the worker thread
    } catch (InterruptedException e) {
      throw new RuntimeException("Could not acquire semaphore for thread " + threadId, e);
    }
    LOGGER.debug("Blocked thread {}", threadId);
    return t;
  }
}
