package com.github.dbmdz.flusswerk.framework.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.model.Envelope;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MessageBrokerTest {

  private MessageBroker messageBroker;

  private Message message;

  private RabbitClient rabbitClient;

  private RoutingProperties routing;

  private FailurePolicy failurePolicy;

  @BeforeEach
  void setUp() throws IOException {
    failurePolicy = new FailurePolicy("some.input.queue");
    routing =
        RoutingProperties.minimal(
            List.of("some.input.queue"), Map.of("default", "some.output.queue"));

    rabbitClient = mock(RabbitClient.class);
    messageBroker = new MessageBroker(routing, rabbitClient);
    message = new Message();
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
  void ack() {
    messageBroker.ack(message);
    verify(rabbitClient).ack(message.getEnvelope());
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
    int numberOfRejections = failurePolicy.getRetries() + 1;
    for (int i = 0; i < numberOfRejections; i++) {
      messageBroker.reject(message);
    }
    FailurePolicy failurePolicy = routing.getFailurePolicy(message);
    verify(rabbitClient).send(anyString(), eq(failurePolicy.getFailedRoutingKey()), eq(message));
  }

  @Test
  @DisplayName("Should send a message to the output queue")
  void sendShouldRouteMessageToOutputQueue() throws IOException {
    messageBroker.send(new Message());
    verify(rabbitClient).send(any(), eq(routing.getOutgoing().get("default")), any());
  }

  @Test
  @DisplayName("Should send multiple messages to the specified queue")
  void sendMultipleMessagesShouldRouteMessagesToSpecifiedQueue() throws IOException {
    String queue = "specified.queue";
    List<Message> messages = Arrays.asList(new Message(), new Message(), new Message());
    messageBroker.send(queue, messages);
    verify(rabbitClient, times(messages.size())).send(any(), eq(queue), any());
  }

  @Test
  @DisplayName("Receive should pull from the specified queue")
  void receiveShouldPullTheSpecifiedQueue() throws InvalidMessageException {
    String queue = "a.very.special.queue";
    messageBroker.receive(queue);
    verify(rabbitClient).receive(queue, false);
  }

  @Test
  @DisplayName("Default receive should pull from the input queue")
  void defaultReceiveShouldPullTheInputQueue() throws InvalidMessageException {
    messageBroker.receive();
    verify(rabbitClient).receive(routing.getIncoming().get(0), false);
  }

  @Test
  @DisplayName("getMessageCount should return all message counts")
  void getMessageCountsShouldGetAllMessageCounts() throws IOException {
    RoutingProperties routing = RoutingProperties.minimal(List.of("input1", "input2"), null);

    Map<String, Long> expected = new HashMap<>();
    expected.put("input1", 100L);
    expected.put("input2", 200L);

    for (String queue : expected.keySet()) {
      when(rabbitClient.getMessageCount(queue)).thenReturn(expected.get(queue));
    }

    messageBroker = new MessageBroker(routing, rabbitClient);
    assertThat(messageBroker.getMessageCounts()).isEqualTo(expected);
  }

  @Test
  @DisplayName("invalidMessage should be ACKed and shifted into failed queue")
  void handleInvalidMessage() throws InvalidMessageException {
    String invalidMessageBody = "invalid";

    Envelope envelope = new Envelope();
    envelope.setDeliveryTag(1);
    envelope.setBody(invalidMessageBody);
    envelope.setSource("some.input.queue");
    when(rabbitClient.receive(eq("some.input.queue"), eq(false)))
        .thenThrow(new InvalidMessageException(envelope, "Invalid message"));

    Assertions.assertThat(messageBroker.receive()).isNull();

    verify(rabbitClient, times(1)).ack(envelope);
    verify(rabbitClient, times(1))
        .sendRaw(anyString(), eq("some.input.queue.failed"), eq(invalidMessageBody.getBytes()));
  }
}
