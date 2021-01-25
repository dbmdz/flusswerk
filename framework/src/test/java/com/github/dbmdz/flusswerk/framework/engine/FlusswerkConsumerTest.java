package com.github.dbmdz.flusswerk.framework.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.dbmdz.flusswerk.framework.TestMessage;
import com.github.dbmdz.flusswerk.framework.jackson.FlusswerkObjectMapper;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.concurrent.PriorityBlockingQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlusswerkConsumerTest {

  private FlusswerkConsumer consumer;
  private PriorityBlockingQueue<Task> taskQueue;
  private FlusswerkObjectMapper flusswerkObjectMapper;

  @BeforeEach
  void setUp() {
    Channel channel = mock(Channel.class);
    taskQueue = new PriorityBlockingQueue<>();
    IncomingMessageType incomingMessageType = new IncomingMessageType(TestMessage.class);
    flusswerkObjectMapper = new FlusswerkObjectMapper(incomingMessageType);
    consumer = new FlusswerkConsumer(channel, flusswerkObjectMapper, "input.queue", 42, taskQueue);
  }

  @Test
  void handleDelivery() throws IOException {
    String consumerTag = "abc";
    Envelope envelope = mock(Envelope.class);
    BasicProperties basicProperties = mock(BasicProperties.class);

    TestMessage message = new TestMessage("bsb12345678");
    Task expected = new Task(message, 42);
    byte[] body = flusswerkObjectMapper.writeValueAsBytes(message);

    consumer.handleDelivery(consumerTag, envelope, basicProperties, body);
    assertThat(taskQueue).hasSize(1);
    assertThat(taskQueue.poll()).isEqualTo(expected);
  }

  @Test
  void deserializeMessage() throws JsonProcessingException {
    String json =
        "{\"id\":\"bsb12345678\",\"envelope\":{\"retries\":0,\"timestamp\":[2021,1,22,14,48,46,792144000],\"source\":\"SOURCE\"}}";
    flusswerkObjectMapper.deserialize(json);
  }

  @Test
  void getInputQueue() {
    assertThat(consumer.getInputQueue()).isEqualTo("input.queue");
  }
}
