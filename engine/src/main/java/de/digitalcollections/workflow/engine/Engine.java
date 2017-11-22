package de.digitalcollections.workflow.engine;

import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Engine {

  private static final Logger LOGGER = LoggerFactory.getLogger(Engine.class);

  private static final int DEFAULT_CONCURRENT_WORKERS = 5;

  private final int concurrentWorkers;

  private final MessageBroker messageBroker;

  private final Flow<?, ?> flow;

  private final ExecutorService executorService;

  private final Semaphore semaphore;

  private boolean running;

  private AtomicInteger activeWorkers;

  public Engine(MessageBroker messageBroker, Flow<?, ?> flow) throws IOException {
    this(messageBroker, flow, DEFAULT_CONCURRENT_WORKERS);
  }

  public Engine(MessageBroker messageBroker, Flow<?, ?> flow, int concurrentWorkers) throws IOException {
    this.messageBroker = messageBroker;
    this.flow = flow;
    this.concurrentWorkers = concurrentWorkers;
    this.executorService = Executors.newFixedThreadPool(concurrentWorkers);
    this.semaphore = new Semaphore(concurrentWorkers);
    this.activeWorkers = new AtomicInteger();

    messageBroker.provideInputQueue(flow.getInputChannel());
    messageBroker.provideOutputQueue(flow.getOutputChannel());
  }

  public void start() {
    LOGGER.debug("Starting engine...");
    running = true;
    while (running) {
      try {
        semaphore.acquire();

        Message message = messageBroker.receive(flow.getInputChannel());

        if (message == null) {
          LOGGER.debug("Checking for new message (available semaphores: {}) - Queue is empty", semaphore.availablePermits());
          TimeUnit.SECONDS.sleep(1);
          semaphore.release();
          continue;
        }

        LOGGER.debug("Checking for new message (available semaphores: {}), got {}", semaphore.availablePermits(), message.getBody());

        executorService.execute(() -> {
          activeWorkers.incrementAndGet();
          process(message);
          activeWorkers.decrementAndGet();
          semaphore.release();
        });

      } catch (IOException | InterruptedException e) {
        LOGGER.error("Got some error", e);
      }
    }
  }

  private void process(Message message) {
    try {
      Message result = flow.process(message);
      if (flow.hasOutputChannel()) {
        messageBroker.send(flow.getOutputChannel(), result);
      }
      messageBroker.ack(message);
    } catch (RuntimeException | IOException e) {
      try {
        LOGGER.error("Could not process message: {}", message.getBody());
        messageBroker.reject(message);
      } catch (IOException e1) {
        LOGGER.error("Could not reject message" + message.getBody(), e1);
      }
    }
  }

  public void createTestMessages(int n) throws IOException {
    for (int i = 0; i < n; i++) {
      String message = String.format("Test message #%d of %d", i, n);
      messageBroker.send(flow.getInputChannel(), new DefaultMessage(message));
    }
  }

  public void stop() {
    running = false;
    LOGGER.debug("Stopping engine...");
  }


  public int getConcurrentWorkers() {
    return concurrentWorkers;
  }

  public int getActiveWorkers() {
    return activeWorkers.get();
  }

  public int getAvailableWorkers() {
    return semaphore.availablePermits();
  }

}
