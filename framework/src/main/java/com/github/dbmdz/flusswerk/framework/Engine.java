package com.github.dbmdz.flusswerk.framework;

import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.DefaultProcessReport;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import java.io.IOException;
import java.util.Collection;
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

  private AtomicInteger activeWorkers;

  private ProcessReport processReport = new DefaultProcessReport();

  /**
   * Creates a new Engine instance with {@value DEFAULT_CONCURRENT_WORKERS} concurrent workers.
   *
   * @param messageBroker the message broker to get messages from or send messages to
   * @param flow the flow to execute agains every message
   * @throws IOException if reading/writing to the message broker fails
   */
  public Engine(MessageBroker messageBroker, Flow flow) throws IOException {
    this(messageBroker, flow, DEFAULT_CONCURRENT_WORKERS, null);
  }

  /**
   * Creates a new Engine instance with a fixed number of concurrent workers.
   *
   * @param messageBroker the message broker to get messages from or send messages to
   * @param flow the flow to execute agains every message
   * @param concurrentWorkers the number of concurrent workers
   * @throws IOException if reading/writing to the message broker fails
   */
  public Engine(MessageBroker messageBroker, Flow flow, int concurrentWorkers) throws IOException {
    this(messageBroker, flow, concurrentWorkers, null);
  }

  /**
   * Creates a new Engine instance with a customized process report.
   *
   * @param messageBroker the message broker to get messages from or send messages to
   * @param flow the flow to execute agains every message
   * @param processReport Reporting implementation
   * @throws IOException if reading/writing to the message broker fails
   */
  public Engine(MessageBroker messageBroker, Flow flow, ProcessReport processReport)
      throws IOException {
    this(messageBroker, flow, DEFAULT_CONCURRENT_WORKERS, null);
  }

  /**
   * Creates a new Engine instance with a fixed number of concurrent workers.
   *
   * @param messageBroker the message broker to get messages from or send messages to
   * @param flow the flow to execute agains every message
   * @param concurrentWorkers the number of concurrent workers
   * @param processReport Reporting implementation (or null, if DefaultProcessReport shall be used)
   * @throws IOException if reading/writing to the message broker fails
   */
  public Engine(
      MessageBroker messageBroker, Flow flow, int concurrentWorkers, ProcessReport processReport)
      throws IOException {
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
  void process(Message receivedMessage) {
    try {
      Collection<? extends Message> messagesToSend = flow.process(receivedMessage);
      if (flow.hasMessagesToSend()) {
        for (Message messageToSend : messagesToSend) {
          messageBroker.send(messageToSend);
        }
      }
      messageBroker.ack(receivedMessage);
      processReport.reportSuccess(receivedMessage);
    } catch (StopProcessingException e) {
      try {
        processReport.reportFail(receivedMessage, e);
        messageBroker.fail(receivedMessage);
      } catch (IOException e1) {
        LOGGER.error("Could not fail message" + receivedMessage.getEnvelope().getBody(), e1);
      }
    } catch (RuntimeException | IOException e) {
      try {
        boolean isRejected = messageBroker.reject(receivedMessage);
        if (isRejected) {
          processReport.reportReject(receivedMessage, e);
        } else {
          processReport.reportFailAfterMaxRetries(receivedMessage, e);
        }
      } catch (IOException e1) {
        LOGGER.error("Could not reject message" + receivedMessage.getEnvelope().getBody(), e1);
      }
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
