package com.github.dbmdz.flusswerk.framework.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.dbmdz.flusswerk.framework.TestMessage;
import com.github.dbmdz.flusswerk.framework.jackson.FlusswerkObjectMapper;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitClient;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import net.logstash.logback.marker.ObjectAppendingMarker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayName("The FlusswerkConsumer")
class FlusswerkConsumerTest {

  private Semaphore availableWorkers;
  private BasicProperties basicProperties;
  private FlusswerkConsumer consumer;
  private PriorityBlockingQueue<Task> taskQueue;
  private FlusswerkObjectMapper flusswerkObjectMapper;
  private Envelope envelope;

  @BeforeEach
  void setUp() {
    availableWorkers = mock(Semaphore.class);
    RabbitClient rabbitClient = mock(RabbitClient.class);
    taskQueue = new PriorityBlockingQueue<>();
    IncomingMessageType incomingMessageType = new IncomingMessageType(TestMessage.class);
    flusswerkObjectMapper = new FlusswerkObjectMapper(incomingMessageType);
    consumer =
        new FlusswerkConsumer(
            availableWorkers, rabbitClient, flusswerkObjectMapper, "input.queue", 42, taskQueue);
    basicProperties = mock(BasicProperties.class);
    envelope = mock(Envelope.class);
  }

  @DisplayName("should handle delivery")
  @Test
  void handleDelivery() throws IOException {
    TestMessage message = new TestMessage("bsb12345678");
    int priority = 42;

    consumer.handleDelivery("consumerTag", envelope, basicProperties, json(message));
    assertThat(taskQueue).hasSize(1);
    Task actual = taskQueue.poll();
    assertThat(actual).isNotNull(); // to prevent NPE warning
    // task.callback breaks actual.equals(expected) here, so we need to compare fields directly
    assertThat(actual.getMessage()).isEqualTo(message);
    assertThat(actual.getPriority()).isEqualTo(priority);
  }

  @DisplayName("should return input queue")
  @Test
  void getInputQueue() {
    assertThat(consumer.getInputQueue()).isEqualTo("input.queue");
  }

  @DisplayName("should set the input queue for each message")
  @Test
  void shouldSetTheInputQueueForMessage() throws IOException {
    TestMessage message = new TestMessage("bsb12345678");

    consumer.handleDelivery("consumerTag", envelope, basicProperties, json(message));

    Task actual = taskQueue.poll();
    assertThat(actual).isNotNull();
    assertThat(actual.getMessage().getEnvelope().getSource()).isEqualTo("input.queue");
  }

  @DisplayName("should set the delivery tag for each message")
  @Test
  void shouldSetDeliveryTag() throws IOException {
    TestMessage message = new TestMessage("bsb12345678");

    when(envelope.getDeliveryTag()).thenReturn(42L);
    consumer.handleDelivery("consumerTag", envelope, basicProperties, json(message));

    Task actual = taskQueue.poll();
    assertThat(actual).isNotNull();
    assertThat(actual.getMessage().getEnvelope().getDeliveryTag())
        .isEqualTo(envelope.getDeliveryTag());
  }

  @DisplayName("should acquire semaphore")
  @Test
  void shouldAcquireSemaphore() throws InterruptedException, IOException {
    TestMessage message = new TestMessage("bsb12345678");
    consumer.handleDelivery("consumerTag", envelope, basicProperties, json(message));
    verify(availableWorkers).acquire();
  }

  private byte[] json(Message message) throws JsonProcessingException {
    return flusswerkObjectMapper.writeValueAsBytes(message);
  }

  static class UndeserializableMessage extends Message {
    UndeserializableMessage() {
      throw new RuntimeException("Exception to prevent deserialization");
    }
  }

  @DisplayName("should use fallback if deserialization fails")
  @Test
  void shouldUseFallbackIfDeserializationFails() {
    Logger logger = (Logger) LoggerFactory.getLogger(FlusswerkConsumer.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    RabbitClient rabbitClient = mock(RabbitClient.class);
    FlusswerkObjectMapper mapper = FlusswerkObjectMapper.forIncoming(UndeserializableMessage.class);

    consumer =
        new FlusswerkConsumer(availableWorkers, rabbitClient, mapper, "input.queue", 42, taskQueue);

    byte[] json = "{\"tracing\": [\"a\"]}".getBytes(StandardCharsets.UTF_8);
    consumer.handleDelivery("consumerTag", envelope, basicProperties, json);

    logger.detachAppender(listAppender);

    assertThat(listAppender.list).hasSize(1);
    assertThat(listAppender.list.get(0).getArgumentArray())
        .containsExactly(new ObjectAppendingMarker("tracing", List.of("a")));
  }

  @DisplayName("should log error if deserialization and fallback fails")
  @Test
  void shouldLogErrorIfDeserializationAndFallbackFails() {
    Logger logger = (Logger) LoggerFactory.getLogger(FlusswerkConsumer.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    RabbitClient rabbitClient = mock(RabbitClient.class);
    FlusswerkObjectMapper mapper = FlusswerkObjectMapper.forIncoming(UndeserializableMessage.class);
    consumer =
        new FlusswerkConsumer(availableWorkers, rabbitClient, mapper, "input.queue", 42, taskQueue);

    byte[] brokenJson = "{\"tracing\": [\"a".getBytes(StandardCharsets.UTF_8);
    consumer.handleDelivery("consumerTag", envelope, basicProperties, brokenJson);

    logger.detachAppender(listAppender);
    assertThat(listAppender.list).hasSize(1);
    assertThat(listAppender.list.get(0).getMessage())
        .contains("Deserialize message fallback failed");
  }
}
