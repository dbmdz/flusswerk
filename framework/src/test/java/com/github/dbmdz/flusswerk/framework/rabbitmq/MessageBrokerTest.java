package com.github.dbmdz.flusswerk.framework.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
  @DisplayName("Should send multiple messages to the specified queue")
  void sendMultipleMessagesShouldRouteMessagesToSpecifiedQueue() throws IOException {
    String queue = "specified.queue";
    List<Message> messages = Arrays.asList(new Message(), new Message(), new Message());
    messageBroker.send(queue, messages);
    verify(rabbitClient, times(messages.size())).send(any(), eq(queue), any());
  }

  @Test
  @DisplayName("Receive should pull from the specified queue")
  void receiveShouldPullTheSpecifiedQueue() throws IOException, InvalidMessageException {
    String queue = "a.very.special.queue";
    messageBroker.receive(queue);
    verify(rabbitClient).receive(queue, false);
  }
}
