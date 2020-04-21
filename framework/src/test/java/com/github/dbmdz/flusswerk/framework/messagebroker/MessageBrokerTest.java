package com.github.dbmdz.flusswerk.framework.messagebroker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.model.DefaultMessage;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MessageBrokerTest {

  private MessageBrokerConfig config = new MessageBrokerConfigImpl();

  private MessageBroker messageBroker;

  private Message message;

  private RabbitClient rabbitClient;

  private RoutingConfigImpl routingConfig;

  @BeforeEach
  void setUp() throws IOException, TimeoutException {
    routingConfig = new RoutingConfigImpl();
    routingConfig.setReadFrom("some.input.queue");
    routingConfig.setWriteTo("some.output.queue");
    FailurePolicy failurePolicy = new FailurePolicy("some.input.queue");
    routingConfig.addFailurePolicy(failurePolicy);
    routingConfig.complete();

    rabbitClient = mock(RabbitClient.class);
    messageBroker = new MessageBroker(config, routingConfig, rabbitClient);
    message = new DefaultMessage("Hey");
    message.getEnvelope().setSource("some.input.queue");
  }

  @Test
  @DisplayName("Send should use the specified routing key")
  void sendShouldUseSpecifiedRoutingKey() throws IOException {
    messageBroker.send("there", message);
    verify(rabbitClient).send(anyString(), eq("there"), eq(message));
  }

  @Test
  @DisplayName("Ack should acknowledge messages")
  void ack() throws IOException {
    messageBroker.ack(message);
    verify(rabbitClient).ack(message);
  }

  @Test
  @DisplayName("Reject should count rejections")
  void rejectShouldCountRejections() throws IOException {
    int numberOfRejections = 3;
    for (int i = 0; i < numberOfRejections; i++) {
      messageBroker.reject(message);
    }
    assertThat(message.getEnvelope().getRetries()).isEqualTo(numberOfRejections);
  }

  @Test
  @DisplayName("Should route a message to the failed queue if it has been rejected to often")
  void rejectShouldRouteToFailedQueueIfMessageIsRejectedTooOften() throws IOException {
    int numberOfRejections = config.getMaxRetries() + 1;
    for (int i = 0; i < numberOfRejections; i++) {
      messageBroker.reject(message);
    }
    FailurePolicy failurePolicy = routingConfig.getFailurePolicy(message);
    verify(rabbitClient).send(anyString(), eq(failurePolicy.getFailedRoutingKey()), eq(message));
  }

  @Test
  @DisplayName("Should send a message to the output queue")
  void sendShouldRouteMessageToOutputQueue() throws IOException {
    messageBroker.send(new DefaultMessage("test"));
    verify(rabbitClient).send(any(), eq(routingConfig.getWriteTo()), any());
  }

  @Test
  @DisplayName("Should send multiple messages to the specified queue")
  void sendMultipleMessagesShouldRouteMessagesToSpecifiedQueue() throws IOException {
    String queue = "specified.queue";
    List<Message> messages =
        Arrays.asList(
            new DefaultMessage("test"), new DefaultMessage("test"), new DefaultMessage("test"));
    messageBroker.send(queue, messages);
    verify(rabbitClient, times(messages.size())).send(any(), eq(queue), any());
  }

  @Test
  @DisplayName("Receive should pull from the specified queue")
  void receiveShouldPullTheSpecifiedQueue() throws IOException, InvalidMessageException {
    String queue = "a.very.special.queue";
    messageBroker.receive(queue);
    verify(rabbitClient).receive(queue);
  }

  @Test
  @DisplayName("Default receive should pull from the input queue")
  void defaultReceiveShouldPullTheInputQueue() throws IOException, InvalidMessageException {
    messageBroker.receive();
    verify(rabbitClient).receive(routingConfig.getReadFrom()[0]);
  }

  @Test
  @DisplayName("getMessageCount should return all message counts")
  void getMessageCountsShouldGetAllMessageCounts() throws IOException {
    RoutingConfigImpl routingConfig = new RoutingConfigImpl();
    routingConfig.setReadFrom("input1", "input2");
    routingConfig.complete();

    Map<String, Long> expected = new HashMap<>();
    expected.put("input1", 100L);
    expected.put("input2", 200L);

    for (String queue : expected.keySet()) {
      when(rabbitClient.getMessageCount(queue)).thenReturn(expected.get(queue));
    }

    messageBroker = new MessageBroker(config, routingConfig, rabbitClient);
    assertThat(messageBroker.getMessageCounts()).isEqualTo(expected);
  }

  @Test
  @DisplayName("isConnectionOk should be true if channel and connection are ok")
  void isConnectionOkShouldBeTrueIfChannelAndConnectionAreOk() throws IOException {
    when(rabbitClient.isChannelAvailable()).thenReturn(true);
    when(rabbitClient.isConnectionOk()).thenReturn(true);
    assertThat(messageBroker.isConnectionOk()).isTrue();
  }

  @Test
  @DisplayName("isConnectionOk should be false if channel is not available")
  void isConnectionOkShouldBeTrueIfChannelIsNotAvailable() throws IOException {
    when(rabbitClient.isChannelAvailable()).thenReturn(false);
    when(rabbitClient.isConnectionOk()).thenReturn(true);
    assertThat(messageBroker.isConnectionOk()).isFalse();
  }

  @Test
  @DisplayName("isConnectionOk should be false if connection is not ok")
  void isConnectionOkShouldBeTrueIfConnectionIsNotOk() throws IOException {
    when(rabbitClient.isChannelAvailable()).thenReturn(true);
    when(rabbitClient.isConnectionOk()).thenReturn(false);
    assertThat(messageBroker.isConnectionOk()).isFalse();
  }

  @Test
  @DisplayName(
      "isConnectionOk should be false if channel is not available and connection is not ok")
  void isConnectionOkShouldBeTrueIfChannelIsNotAvailableAndConnectionIsNotOk() throws IOException {
    when(rabbitClient.isChannelAvailable()).thenReturn(false);
    when(rabbitClient.isConnectionOk()).thenReturn(false);
    assertThat(messageBroker.isConnectionOk()).isFalse();
  }

  @Test
  @DisplayName("invalidMessage should be ACKed and shifted into fqiled queue")
  void handleInvalidMessage() throws IOException, InvalidMessageException {
    String invalidMessageBody = "invalid";

    Message invalidMessage = new DefaultMessage();
    invalidMessage.getEnvelope().setDeliveryTag(1);
    invalidMessage.getEnvelope().setBody(invalidMessageBody);
    invalidMessage.getEnvelope().setSource("some.input.queue");
    when(rabbitClient.receive(eq("some.input.queue")))
        .thenThrow(new InvalidMessageException(invalidMessage, "Invalid message"));

    Assertions.assertThat(messageBroker.receive()).isNull();

    verify(rabbitClient, times(1)).ack(any(Message.class));
    verify(rabbitClient, times(1))
        .sendRaw(anyString(), eq("some.input.queue.failed"), eq(invalidMessageBody.getBytes()));
  }
}
