package com.github.dbmdz.flusswerk.framework.engine;

import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.DefaultProcessReport;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Run flows {@link Flow} for every message from the {@link MessageBroker} - usually several in
 * parallel.
 */
public class Engine {

  private static final Logger LOGGER = LoggerFactory.getLogger(Engine.class);

  private static final int DEFAULT_CONCURRENT_WORKERS = 5;

  private final int concurrentWorkers;

  private final MessageBroker messageBroker;

  private final Flow flow;

  private final ExecutorService executorService;

  private final Semaphore semaphore;

  private boolean running;

  private final AtomicInteger activeWorkers;

  private ProcessReport processReport = new DefaultProcessReport();

  /**
   * Creates a new Engine instance with {@value DEFAULT_CONCURRENT_WORKERS} concurrent workers.
   *
   * @param messageBroker the message broker to get messages from or send messages to
   * @param flow the flow to execute agains every message
   */
  public Engine(MessageBroker messageBroker, Flow flow) {
    this(messageBroker, flow, DEFAULT_CONCURRENT_WORKERS, null);
  }

  /**
   * Creates a new Engine instance with a fixed number of concurrent workers.
   *
   * @param messageBroker the message broker to get messages from or send messages to
   * @param flow the flow to execute agains every message
   * @param concurrentWorkers the number of concurrent workers
   */
  public Engine(MessageBroker messageBroker, Flow flow, int concurrentWorkers) {
    this(messageBroker, flow, concurrentWorkers, null);
  }

  /**
   * Creates a new Engine instance with a customized process report.
   *
   * @param messageBroker the message broker to get messages from or send messages to
   * @param flow the flow to execute agains every message
   * @param processReport Reporting implementation
   */
  public Engine(MessageBroker messageBroker, Flow flow, ProcessReport processReport) {
    this(messageBroker, flow, DEFAULT_CONCURRENT_WORKERS, processReport);
  }

  /**
   * Creates a new Engine instance with a fixed number of concurrent workers.
   *
   * @param messageBroker the message broker to get messages from or send messages to
   * @param flow the flow to execute agains every message
   * @param concurrentWorkers the number of concurrent workers
   * @param processReport Reporting implementation (or null, if DefaultProcessReport shall be used)
   */
  public Engine(
      MessageBroker messageBroker, Flow flow, int concurrentWorkers, ProcessReport processReport) {
    this.messageBroker = messageBroker;
    this.flow = flow;
    this.concurrentWorkers = concurrentWorkers;
    this.executorService = Executors.newFixedThreadPool(concurrentWorkers);
    this.semaphore = new Semaphore(concurrentWorkers);
    this.activeWorkers = new AtomicInteger();
    if (processReport != null) {
      this.processReport = processReport;
    }
  }

  /**
   * Starts processing messages until {@link Engine#stop()} is called. If there are no messages in
   * the input queue, the engine waits for new messages to arrive.
   */
  public void start() {
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
  }

  @SuppressWarnings("unchecked")
  void process(Message message) {
    Collection<? extends Message> messagesToSend = Collections.emptyList();
    try {
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
      messageBroker.send(messagesToSend);
      messageBroker.ack(message);
      processReport.reportSuccess(message);
    } catch (Exception e) {
      var stopProcessingException =
          new StopProcessingException("Could not finish message handling").causedBy(e);
      fail(message, stopProcessingException);
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
    } catch (IOException fatalExecption) {
      var body = receivedMessage.getEnvelope().getBody();
      LOGGER.error("Could not reject message" + body, fatalExecption);
    }
  }

  private void fail(Message message, StopProcessingException e) {
    try {
      processReport.reportFail(message, e);
      messageBroker.fail(message);
    } catch (IOException fatalExecption) {
      var body = message.getEnvelope().getBody();
      LOGGER.error("Could not fail message" + body, fatalExecption);
    }
  }

  /**
   * Stops processing new messages or waiting for new messages to arrive. This usually means that
   * the application will shut down when the last worker finished.
   */
  public void stop() {
    running = false;
    LOGGER.debug("Stopping engine...");
  }

  /** @return statistics about the engine's state like active workers */
  public EngineStats getStats() {
    return new EngineStats(concurrentWorkers, activeWorkers.get(), semaphore.availablePermits());
  }
}
