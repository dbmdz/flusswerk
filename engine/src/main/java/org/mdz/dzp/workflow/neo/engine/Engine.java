package org.mdz.dzp.workflow.neo.engine;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.mdz.dzp.workflow.neo.engine.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Engine {

  private static final Logger LOGGER = LoggerFactory.getLogger(Engine.class);

  private final RabbitMQ rabbitMQ;

  private final Flow<?, ?> flow;

  private final ExecutorService executorService;

  private final Semaphore semaphore;

  public Engine(RabbitMQ rabbitMQ, Flow<?, ?> flow) throws IOException {
    this.rabbitMQ = rabbitMQ;
    this.flow = flow;
    this.executorService = Executors.newFixedThreadPool(5);
    this.semaphore = new Semaphore(5);

    rabbitMQ.provideInputQueue(flow.getInputChannel());
    rabbitMQ.provideOutputQueue(flow.getOutputChannel());
  }

  public void start() {
    while (true) {
      try {
        semaphore.acquire();

        Message message = rabbitMQ.receive(flow.getInputChannel());

        if (message == null) {
          LOGGER.info("Checking for new message (available semaphores: {}) - Queue is empty", semaphore.availablePermits());
          TimeUnit.SECONDS.sleep(1);
          semaphore.release();
          continue;
        }

        LOGGER.info("Checking for new message (available semaphores: {}), got {}", semaphore.availablePermits(), message.getBody());

        executorService.execute(() -> {
          try {
            Message result = flow.process(message);
            if (flow.hasOutputChannel()) {
              rabbitMQ.send(flow.getOutputChannel(), result);
            }
            rabbitMQ.ack(message);
          } catch (RuntimeException | IOException  e) {
            try {
              LOGGER.error("Could not process message: {}", message.getBody());
              rabbitMQ.reject(message);

            } catch (IOException e1) {
              LOGGER.error("Could not reject message" + message.getBody(), e1);
            }
          }
          semaphore.release();
        });

      } catch (IOException | InterruptedException e) {
        LOGGER.error("Got some error", e);
      }
    }
  }

  public void createTestMessages() throws IOException {
    final int n = 500;
    for (int i = 0; i < n; i++) {
      String message = String.format("Test message #%d of %d", i, n);
      rabbitMQ.send(flow.getInputChannel(), new Message(message));
    }
  }

}
