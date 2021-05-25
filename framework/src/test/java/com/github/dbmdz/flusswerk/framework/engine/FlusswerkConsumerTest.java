package com.github.dbmdz.flusswerk.framework.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.dbmdz.flusswerk.framework.TestMessage;
import com.github.dbmdz.flusswerk.framework.jackson.FlusswerkObjectMapper;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    Channel channel = mock(Channel.class);
    taskQueue = new PriorityBlockingQueue<>();
    IncomingMessageType incomingMessageType = new IncomingMessageType(TestMessage.class);
    flusswerkObjectMapper = new FlusswerkObjectMapper(incomingMessageType);
    consumer =
        new FlusswerkConsumer(
            availableWorkers, channel, flusswerkObjectMapper, "input.queue", 42, taskQueue);
    basicProperties = mock(BasicProperties.class);
    envelope = mock(Envelope.class);
  }

  @DisplayName("should handle delivery")
  @Test
  void handleDelivery() throws IOException {
    TestMessage message = new TestMessage("bsb12345678");
    Task expected = new Task(message, 42);

    consumer.handleDelivery("consumerTag", envelope, basicProperties, json(message));
    assertThat(taskQueue).hasSize(1);
    assertThat(taskQueue.poll()).isEqualTo(expected);
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
}
