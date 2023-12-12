package com.github.dbmdz.flusswerk.framework.engine;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.github.dbmdz.flusswerk.framework.jackson.FlusswerkObjectMapper;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitClient;
import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receive AMQP message from a RabbitMQ queue, deserialize the Flusswerk {@link Message} and put
 * that into the internal task queue.
 */
public class FlusswerkConsumer implements Consumer {

  public static final FlusswerkObjectMapper FALLBACK_MAPPER =
      new FlusswerkObjectMapper(new IncomingMessageType());
  private static final Logger LOGGER = LoggerFactory.getLogger(FlusswerkConsumer.class);
  private volatile String _consumerTag;

  private final Semaphore availableWorkers;
  private final RabbitClient rabbitClient;
  private final FlusswerkObjectMapper flusswerkObjectMapper;
  private final PriorityBlockingQueue<Task> taskQueue;
  private final int priority;
  private final String inputQueue;

  /**
   * Constructs a new instance and records its association to the passed-in channel.
   *
   * @param rabbitClient the client which handles communication with RabbitMQ
   * @param flusswerkObjectMapper the object mapper to deserialize messages
   * @param inputQueue the rabbitMQ queue this consumer is bound to
   */
  public FlusswerkConsumer(
      Semaphore availableWorkers,
      RabbitClient rabbitClient,
      FlusswerkObjectMapper flusswerkObjectMapper,
      String inputQueue,
      int priority,
      PriorityBlockingQueue<Task> taskQueue) {
    this.availableWorkers = availableWorkers;
    this.rabbitClient = rabbitClient;
    this.flusswerkObjectMapper = flusswerkObjectMapper;
    this.inputQueue = inputQueue;
    this.priority = priority;
    this.taskQueue = taskQueue;
  }

  @Override
  public void handleConsumeOk(String consumerTag) {
    this._consumerTag = consumerTag;
  }

  @Override
  public void handleCancelOk(String consumerTag) {
    // nothing to do
  }

  @Override
  public void handleCancel(String consumerTag) {
    // nothing to do
  }

  @Override
  public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
    // nothing to do
  }

  @Override
  public void handleRecoverOk(String consumerTag) {
    // nothing to do
  }

  public String getConsumerTag() {
    return this._consumerTag;
  }

  @Override
  public void handleDelivery(
      String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) {

    try {
      availableWorkers.acquire();
    } catch (InterruptedException e) {
      // If waiting for the semaphore is interrupted (e.g. because of shutdown), the current message
      // should not be processed at all.
      LOGGER.warn("FlusswerkConsumer interrupted while waiting for free worker", e);
      rabbitClient.reject(envelope);
      return;
    }

    String json = new String(body, StandardCharsets.UTF_8);
    try {
      Message message = flusswerkObjectMapper.deserialize(json);
      message.getEnvelope().setSource(inputQueue);
      message.getEnvelope().setDeliveryTag(envelope.getDeliveryTag());
      taskQueue.put(new Task(message, priority, availableWorkers::release));
    } catch (Exception e) {
      List<String> tracing = null;
      try {
        Message fallbackMessage = FALLBACK_MAPPER.deserialize(json);
        tracing = fallbackMessage.getTracing();
      } catch (Exception exception) {
        LOGGER.error("Deserialize message fallback failed, too", exception);
      }
      // if there is no tracing, then the exception above already has been logged
      if (tracing != null) {
        LOGGER.error("Could not deserialize message", kv("tracing", tracing), e);
      }
      rabbitClient.ack(envelope.getDeliveryTag());
      availableWorkers.release();
    }
  }

  public String getInputQueue() {
    return inputQueue;
  }
}
