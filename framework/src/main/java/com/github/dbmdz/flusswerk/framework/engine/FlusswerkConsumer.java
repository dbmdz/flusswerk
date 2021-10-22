package com.github.dbmdz.flusswerk.framework.engine;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.github.dbmdz.flusswerk.framework.jackson.FlusswerkObjectMapper;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
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
public class FlusswerkConsumer extends DefaultConsumer {

  public static final FlusswerkObjectMapper FALLBACK_MAPPER =
      new FlusswerkObjectMapper(new IncomingMessageType());
  private static final Logger LOGGER = LoggerFactory.getLogger(FlusswerkConsumer.class);

  private final Semaphore availableWorkers;
  private final Channel channel;
  private final FlusswerkObjectMapper flusswerkObjectMapper;
  private final PriorityBlockingQueue<Task> taskQueue;
  private final int priority;
  private final String inputQueue;

  /**
   * Constructs a new instance and records its association to the passed-in channel.
   *
   * @param channel the channel to which this consumer is attached
   * @param flusswerkObjectMapper the object mapper to deserialize messages
   * @param inputQueue the rabbitMQ queue this consumer is bound to
   */
  public FlusswerkConsumer(
      Semaphore availableWorkers,
      Channel channel,
      FlusswerkObjectMapper flusswerkObjectMapper,
      String inputQueue,
      int priority,
      PriorityBlockingQueue<Task> taskQueue) {
    super(channel);
    this.availableWorkers = availableWorkers;
    this.channel = channel;
    this.flusswerkObjectMapper = flusswerkObjectMapper;
    this.inputQueue = inputQueue;
    this.priority = priority;
    this.taskQueue = taskQueue;
  }

  @Override
  public void handleDelivery(
      String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
      throws IOException {

    try {
      availableWorkers.acquire();
    } catch (InterruptedException e) {
      // If waiting for the semaphore is interrupted (e.g. because of shutdown), the current message
      // should not be processed at all.
      LOGGER.warn("FlusswerkConsumer interrupted while waiting for free worker", e);
      channel.basicReject(envelope.getDeliveryTag(), true);
      return;
    }

    String json = new String(body, StandardCharsets.UTF_8);
    try {
      Message message = flusswerkObjectMapper.deserialize(json);
      message.getEnvelope().setSource(inputQueue);
      message.getEnvelope().setDeliveryTag(envelope.getDeliveryTag());
      taskQueue.put(new Task(message, priority));
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
      channel.basicAck(envelope.getDeliveryTag(), false);
      availableWorkers.release();
    }
  }

  public String getInputQueue() {
    return inputQueue;
  }
}
