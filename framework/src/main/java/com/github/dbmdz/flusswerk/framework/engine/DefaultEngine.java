package com.github.dbmdz.flusswerk.framework.engine;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.MessageBroker;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Run flows {@link Flow} for every message from the {@link MessageBroker} - usually several in
 * parallel.
 */
public class DefaultEngine implements Engine {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEngine.class);

  private final int concurrentWorkers;

  private final MessageBroker messageBroker;

  private final Flow flow;

  private final ExecutorService executorService;

  private final Semaphore semaphore;

  private final AtomicInteger activeWorkers;

  private final ProcessReport processReport;

  private final ReentrantLock engineStarted;

  private boolean running;
  private Tracing tracing;

  /**
   * Creates a new Engine instance with a fixed number of concurrent workers.
   *
   * @param messageBroker the message broker to get messages from or send messages to
   * @param flow the flow to execute for every message
   * @param concurrentWorkers the number of concurrent workers
   * @param processReport Reporting implementation (or null, if DefaultProcessReport shall be used)
   */
  public DefaultEngine(
      MessageBroker messageBroker,
      Flow flow,
      int concurrentWorkers,
      ProcessReport processReport,
      Tracing tracing) {
    this.messageBroker = messageBroker;
    this.flow = flow;
    this.concurrentWorkers = concurrentWorkers;
    this.executorService = Executors.newFixedThreadPool(concurrentWorkers);
    this.semaphore = new Semaphore(concurrentWorkers);
    this.activeWorkers = new AtomicInteger();
    this.processReport = processReport;
    this.tracing = requireNonNull(tracing);
    engineStarted = new ReentrantLock();
    running = false;
  }

  /**
   * Starts processing messages until {@link DefaultEngine#stop()} is called. If there are no
   * messages in the input queue, the engine waits for new messages to arrive.
   */
  @Override
  public void start() {
    boolean couldLock;
    try {
      couldLock = engineStarted.tryLock(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException("Could not start engine, locking failed", e);
    }
    if (!couldLock) {
      throw new IllegalStateException("Engine has already be started");
    }
    LOGGER.debug("Starting engine...");
    running = true;
    while (running) {
      try {
        semaphore.acquire();

        Message message = messageBroker.receive();

        if (message == null) {
          LOGGER.debug(
              "Checking for new message (available semaphores: {}) - Queue is empty",
              semaphore.availablePermits());
          TimeUnit.SECONDS.sleep(1);
          semaphore.release();
          continue;
        }

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Checking for new message (available semaphores: {}), got {}",
              semaphore.availablePermits(),
              message.getEnvelope().getBody());
        }

        executorService.execute(
            () -> {
              activeWorkers.incrementAndGet();
              process(message);
              activeWorkers.decrementAndGet();
              semaphore.release();
            });

      } catch (IOException | InterruptedException e) {
        LOGGER.error("Got some error: " + e, e);
      }
    }
    engineStarted.unlock(); // Engine successfully stopped, could now be started again
  }

  @Override
  public void process(Message message) {
    Collection<? extends Message> messagesToSend;
    try {
      tracing.register(message.getTracing());
      messagesToSend = flow.process(message);
    } catch (StopProcessingException e) {
      fail(message, e);
      return; // processing was not successful → stop here
    } catch (RuntimeException e) {
      retryOrFail(message, e);
      return; // processing was not successful → stop here
    }

    // Data processing was successful, now handle the messaging
    try {
      if (!messagesToSend.isEmpty()) {
        messageBroker.send(messagesToSend);
      }
      messageBroker.ack(message);
      processReport.reportSuccess(message);
    } catch (Exception e) {
      var stopProcessingException =
          new StopProcessingException("Could not finish message handling").causedBy(e);
      fail(message, stopProcessingException);
    } finally {
      tracing.deregister();
    }
  }

  private void retryOrFail(Message receivedMessage, RuntimeException e) {
    try {
      boolean isRejected = messageBroker.reject(receivedMessage);
      if (isRejected) {
        processReport.reportReject(receivedMessage, e);
      } else {
        processReport.reportFailAfterMaxRetries(receivedMessage, e);
      }
    } catch (IOException fatalException) {
      var body = receivedMessage.getEnvelope().getBody();
      LOGGER.error("Could not reject message" + body, fatalException);
    }
  }

  private void fail(Message message, StopProcessingException e) {
    try {
      processReport.reportFail(message, e);
      messageBroker.fail(message);
    } catch (IOException fatalException) {
      var body = message.getEnvelope().getBody();
      LOGGER.error("Could not fail message" + body, fatalException);
    }
  }

  /**
   * Stops processing new messages or waiting for new messages to arrive. This usually means that
   * the application will shut down when the last worker finished.
   */
  @Override
  public void stop() {
    running = false;
    LOGGER.debug("Stopping engine...");
  }

  /** @return statistics about the engine's state like active workers */
  @Override
  public EngineStats getStats() {
    return new EngineStats(concurrentWorkers, activeWorkers.get(), semaphore.availablePermits());
  }
}
