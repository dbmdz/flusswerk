package com.github.dbmdz.flusswerk.framework.engine;

import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Run flows {@link Flow} for every message from the {@link RabbitClient} - usually several in
 * parallel.
 */
public class Engine implements ChannelListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(Engine.class);

  private final ExecutorService executorService;
  private final List<Worker> workers;
  private final List<FlusswerkConsumer> consumers;
  private final RabbitClient rabbitClient;
  private final Semaphore startOnlyOnce;

  /**
   * Creates a new Engine bridging RabbitMQ consumers and Flusswerk workers.
   *
   * @param rabbitClient the RabbitMQ client that registers consumers to a given channel
   * @param flusswerkConsumers the consumers that read those messages from RabbitMQ
   * @param workers the workers that do the processing
   */
  public Engine(
      RabbitClient rabbitClient, List<FlusswerkConsumer> flusswerkConsumers, List<Worker> workers) {
    this(rabbitClient, flusswerkConsumers, workers, Executors.newFixedThreadPool(workers.size()));
  }

  public Engine(
      RabbitClient rabbitClient,
      List<FlusswerkConsumer> flusswerkConsumers,
      List<Worker> workers,
      ExecutorService executorService) {
    this.rabbitClient = rabbitClient;
    this.executorService = executorService;
    this.workers = workers;
    this.consumers = flusswerkConsumers;
    this.startOnlyOnce = new Semaphore(1);
  }

  /**
   * Starts processing messages until {@link Engine#stop()} is called. If there are no messages in
   * the input queue, the engine waits for new messages to arrive.
   */
  public void start() {
    if (!startOnlyOnce.tryAcquire()) {
      LOGGER.error("Engine had already been started once. Starting again is not possible.");
      return;
    }

    LOGGER.debug("Starting worker threads");
    for (Worker worker : workers) {
      LOGGER.debug("Starting worker {}", worker);
      executorService.execute(worker);
    }

    LOGGER.debug("Starting consumers");
    for (FlusswerkConsumer consumer : consumers) {
      rabbitClient.consume(consumer, false);
    }

    // keep track of channel resets
    this.rabbitClient.addChannelListener(this);
  }

  /**
   * Stops processing new messages or waiting for new messages to arrive. This usually means that
   * the application will shut down when the last worker finished.
   */
  public void stop() {
    // Stop receiving new messages
    consumers.forEach(
        consumer -> {
          try {
            rabbitClient.cancel(consumer.getConsumerTag());
          } catch (IOException e) {
            LOGGER.error("Could not cancel consumer", e);
          }
        });

    // Drain internal task queue
    PriorityBlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>();
    List<Task> remainingTasks = new ArrayList<>();
    taskQueue.drainTo(remainingTasks);

    // NACK and requeue all messages that have not be processed yet
    for (var task : remainingTasks) {
      long deliveryTag = task.getMessage().getEnvelope().getDeliveryTag();
      try {
        rabbitClient.nack(deliveryTag, false, true);
      } catch (IOException e) {
        LOGGER.error("Could not NACK message with delivery tag {}", deliveryTag, e);
      }
    }

    // Wait for workers to stop
    workers.forEach(Worker::stop);
    executorService.shutdown();
    try {
      boolean shutdownSuccessful = executorService.awaitTermination(5, TimeUnit.MINUTES);
      if (!shutdownSuccessful) {
        LOGGER.error("Not all workers did terminate during shutdown window of 5 minutes");
      }
    } catch (InterruptedException e) {
      LOGGER.error("Timeout awaiting worker shutdown after 5 minutes", e);
    }
  }

  @Override
  public void handleReset() {
    LOGGER.debug("Register consumers again after channel reset");
    for (FlusswerkConsumer consumer : consumers) {
      rabbitClient.consume(consumer, false);
    }
  }
}
