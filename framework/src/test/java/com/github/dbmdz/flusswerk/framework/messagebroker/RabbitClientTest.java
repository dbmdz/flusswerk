package com.github.dbmdz.flusswerk.framework.messagebroker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dbmdz.flusswerk.framework.CustomMessage;
import com.github.dbmdz.flusswerk.framework.CustomMessageMixin;
import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.jackson.SingleClassModule;
import com.github.dbmdz.flusswerk.framework.model.Envelope;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RabbitClientTest {

  private final MessageBrokerConfig<Message> config = new MessageBrokerConfig<>(Message.class);

  private RabbitConnection connection;

  private Channel channel;

  private Message message;

  private RabbitClient<Message> rabbitClient;

  @BeforeEach
  void setUp() {
    connection = mock(RabbitConnection.class);
    channel = mock(Channel.class);
    when(connection.getChannel()).thenReturn(channel);
    message = new Message("Hey");
    rabbitClient = new RabbitClient<>(config, connection);
  }

  @Test
  void ack() throws IOException {
    message.getEnvelope().setDeliveryTag(123123123);
    rabbitClient.ack(message);
    verify(channel).basicAck(eq(message.getEnvelope().getDeliveryTag()), eq(false));
  }

  @Test
  void sendShouldUseCorrectRoutingKey() throws IOException {
    rabbitClient.send("workflow", "there", message);
    verify(channel).basicPublish(anyString(), eq("there"), any(), any(byte[].class));
  }

  @Test
  void shouldWorkWithCustomMessageType() throws IOException {
    MessageBrokerConfig<CustomMessage> config = new MessageBrokerConfig<>(CustomMessage.class);
    config.addJacksonModule(new SingleClassModule(CustomMessage.class, CustomMessageMixin.class));
    config.setMessageClass(CustomMessage.class);
    RabbitClient<CustomMessage> rabbitClient = new RabbitClient<>(config, connection);
    CustomMessage message = new CustomMessage();
    message.setCustomField("Blah!");
    CustomMessage recreated =
        rabbitClient.deserialize(
            new String(rabbitClient.serialize(message), StandardCharsets.UTF_8));
    assertThat(message.getCustomField()).isEqualTo(recreated.getCustomField());
  }

  @Test
  void receiveShouldPullTheInputQueue() throws IOException, InvalidMessageException {
    long deliveryTag = 476253;
    String tracingId = "123123123";
    int retries = 333;
    LocalDateTime timestamp = LocalDateTime.now();
    String inputQueue = "some.input.queue";

    Message messageToReceive = createMessage(tracingId, retries, timestamp);
    GetResponse response = createResponse(deliveryTag, messageToReceive, rabbitClient);

    String body = new String(response.getBody(), StandardCharsets.UTF_8);

    when(channel.basicGet(inputQueue, false)).thenReturn(response);
    Message message = rabbitClient.receive(inputQueue);
    assertThat(message.getEnvelope())
        .returns(body, from(Envelope::getBody))
        .returns(deliveryTag, from(Envelope::getDeliveryTag))
        .returns(retries, from(Envelope::getRetries))
        .returns(timestamp, from(Envelope::getTimestamp));
    assertThat(message).returns(tracingId, from(Message::getTracingId));
  }

  private Message createMessage(String tracingId, int retries, LocalDateTime timestamp) {
    Message messageToReceive = new Message(tracingId);
    messageToReceive.getEnvelope().setRetries(retries);
    messageToReceive.getEnvelope().setTimestamp(timestamp);
    return messageToReceive;
  }

  private GetResponse createResponse(
      long deliveryTag, Message message, RabbitClient<Message> rabbitClient) throws IOException {
    com.rabbitmq.client.Envelope envelope =
        new com.rabbitmq.client.Envelope(deliveryTag, true, "workflow", "some.input.queue");
    BasicProperties basicProperties = new BasicProperties.Builder().build();
    return new GetResponse(envelope, basicProperties, rabbitClient.serialize(message), 1);
  }

  @Test
  @DisplayName("getMessageCount should return message count for queue")
  void getMessageCountShouldReturnMessageCount() throws IOException {
    when(channel.messageCount("test")).thenReturn(123L);
    assertThat(rabbitClient.getMessageCount("test")).isEqualTo(123L);
  }

  @Test
  @DisplayName("isConnectionOk should return if connection is ok")
  void isConnectionOk() {
    when(connection.isOk()).thenReturn(true, false);
    assertThat(rabbitClient.isConnectionOk()).isTrue();
    assertThat(rabbitClient.isConnectionOk()).isFalse();
  }

  @Test
  @DisplayName("isChannelAvailable should return if channel is available")
  void isChannelAvailable() {
    when(channel.isOpen()).thenReturn(true, false);
    assertThat(rabbitClient.isChannelAvailable()).isTrue();
    assertThat(rabbitClient.isChannelAvailable()).isFalse();
  }

  @Test
  @DisplayName("Invalid JSON message throws an InvalidMessageException")
  void shiftInvalidMessageToFailedQueue() throws IOException {
    final String queueName = "test";
    final String failedQueueName = queueName + ".failed";
    final String retryQueueName = queueName + ".retry";

    GetResponse getResponse = mock(GetResponse.class);
    when(getResponse.getBody()).thenReturn("NoValidJson".getBytes());
    com.rabbitmq.client.Envelope envelope = mock(com.rabbitmq.client.Envelope.class);
    when(envelope.getDeliveryTag()).thenReturn(1L);
    when(getResponse.getEnvelope()).thenReturn(envelope);
    when(channel.basicGet(queueName, false)).thenReturn(getResponse);

    InvalidMessageException thrown =
        assertThrows(InvalidMessageException.class, () -> rabbitClient.receive("test"));
    assertThat(thrown.getMessage()).startsWith("Unrecognized token 'NoValidJson'");
  }
}
