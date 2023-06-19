package com.github.dbmdz.flusswerk.framework.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.dbmdz.flusswerk.framework.TestMessage;
import com.github.dbmdz.flusswerk.framework.engine.FlusswerkConsumer;
import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.model.Envelope;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.RecoverableChannel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RabbitClientTest {

  private RabbitConnection connection;

  private Channel channel;

  private Message message;

  @BeforeEach
  void setUp() {
    connection = mock(RabbitConnection.class);
    channel = mock(RecoverableChannel.class);
    when(connection.getChannel()).thenReturn(channel);
    message = new Message();
  }

  @Test
  void ack() throws IOException {
    RabbitClient rabbitClient = new RabbitClient(connection);
    message.getEnvelope().setDeliveryTag(123123123);
    rabbitClient.ack(message.getEnvelope());
    verify(channel).basicAck(eq(message.getEnvelope().getDeliveryTag()), eq(false));
  }

  @Test
  void nack() throws IOException {
    RabbitClient rabbitClient = new RabbitClient(connection);
    rabbitClient.nack(123123123, false, false);
    verify(channel).basicNack(123123123, false, false);
  }

  @Test
  void cancel() throws IOException {
    RabbitClient rabbitClient = new RabbitClient(connection);
    rabbitClient.cancel("my-consumer-tag");
    verify(channel).basicCancel("my-consumer-tag");
  }

  @Test
  void consume() throws IOException {
    RabbitClient rabbitClient = new RabbitClient(connection);
    FlusswerkConsumer consumer = mock(FlusswerkConsumer.class);
    when(consumer.getInputQueue()).thenReturn("input");
    rabbitClient.consume(consumer, true);
    verify(channel).basicConsume("input", true, consumer);
  }

  @Test
  void sendShouldUseCorrectRoutingKey() throws IOException {
    RabbitClient rabbitClient = new RabbitClient(connection);
    rabbitClient.send("workflow", "there", message);
    verify(channel).basicPublish(anyString(), eq("there"), any(), any(byte[].class));
  }

  interface TestMessageMixin {
    @JsonIgnore
    List<String> getValues();
  }

  @Test
  void shouldWorkWithCustomMessageType() throws IOException {
    var messageImplementation = new IncomingMessageType(TestMessage.class, TestMessageMixin.class);
    RabbitClient client = new RabbitClient(messageImplementation, connection);

    TestMessage message = new TestMessage("abc123", "should be ignored");
    byte[] serialized = client.serialize(message);

    TestMessage expected = new TestMessage("abc123");
    Message actual = client.deserialize(new String(serialized));

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void receiveShouldPullTheInputQueue() throws IOException, InvalidMessageException {
    RabbitClient rabbitClient = new RabbitClient(connection);

    long deliveryTag = 476253;
    int retries = 333;
    Instant created = Instant.now();
    String inputQueue = "some.input.queue";

    Message messageToReceive = createMessage(retries, created);
    GetResponse response = createResponse(deliveryTag, messageToReceive, rabbitClient);

    String body = new String(response.getBody(), StandardCharsets.UTF_8);

    when(channel.basicGet(inputQueue, false)).thenReturn(response);
    Message message = rabbitClient.receive(inputQueue, false);
    assertThat(message.getEnvelope())
        .returns(body, from(Envelope::getBody))
        .returns(deliveryTag, from(Envelope::getDeliveryTag))
        .returns(retries, from(Envelope::getRetries))
        .returns(created, from(Envelope::getCreated));
  }

  private Message createMessage(int retries, Instant created) {
    Message messageToReceive = new Message();
    messageToReceive.getEnvelope().setRetries(retries);
    messageToReceive.getEnvelope().setCreated(created);
    return messageToReceive;
  }

  private GetResponse createResponse(long deliveryTag, Message message, RabbitClient rabbitClient)
      throws IOException {
    com.rabbitmq.client.Envelope envelope =
        new com.rabbitmq.client.Envelope(deliveryTag, true, "workflow", "some.input.queue");
    BasicProperties basicProperties = new BasicProperties.Builder().build();
    return new GetResponse(envelope, basicProperties, rabbitClient.serialize(message), 1);
  }

  @Test
  @DisplayName("getMessageCount should return message count for queue")
  void getMessageCountShouldReturnMessageCount() throws IOException {
    when(channel.messageCount("test")).thenReturn(123L);
    RabbitClient rabbitClient = new RabbitClient(connection);
    assertThat(rabbitClient.getMessageCount("test")).isEqualTo(123L);
  }

  @Test
  @DisplayName("isChannelAvailable should return if channel is available")
  void isChannelAvailable() {
    when(channel.isOpen()).thenReturn(true, false);
    RabbitClient rabbitClient = new RabbitClient(connection);
    assertThat(rabbitClient.isChannelAvailable()).isTrue();
    assertThat(rabbitClient.isChannelAvailable()).isFalse();
  }

  @Test
  @DisplayName("Invalid JSON message throws an InvalidMessageException")
  void shiftInvalidMessageToFailedQueue() throws IOException {
    final String queueName = "test";

    GetResponse getResponse = mock(GetResponse.class);
    when(getResponse.getBody()).thenReturn("NoValidJson".getBytes());
    com.rabbitmq.client.Envelope envelope = mock(com.rabbitmq.client.Envelope.class);
    when(envelope.getDeliveryTag()).thenReturn(1L);
    when(getResponse.getEnvelope()).thenReturn(envelope);
    when(channel.basicGet(queueName, false)).thenReturn(getResponse);

    RabbitClient rabbitClient = new RabbitClient(connection);

    InvalidMessageException thrown =
        assertThrows(InvalidMessageException.class, () -> rabbitClient.receive("test", false));
    assertThat(thrown.getMessage()).startsWith("Unrecognized token 'NoValidJson'");
  }
}
