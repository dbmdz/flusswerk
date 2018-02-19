package de.digitalcollections.flusswerk.engine.messagebroker;

import de.digitalcollections.flusswerk.engine.model.DefaultMessage;
import de.digitalcollections.flusswerk.engine.model.Message;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    List<Message> messages = Arrays.asList(
        new DefaultMessage("test"),
        new DefaultMessage("test"),
        new DefaultMessage("test"));
    messageBroker.send(queue, messages);
    verify(rabbitClient, times(messages.size())).send(any(), eq(queue), any());
  }

  @Test
  @DisplayName("Receive should pull from the specified queue")
  void receiveShouldPullTheSpecifiedQueue() throws IOException {
    String queue = "a.very.special.queue";
    messageBroker.receive(queue);
    verify(rabbitClient).receive(queue);
  }

  @Test
  @DisplayName("Default receive should pull from the input queue")
  void defaultReceiveShouldPullTheInputQueue() throws IOException {
    messageBroker.receive();
    verify(rabbitClient).receive(routingConfig.getReadFrom()[0]);
  }

}
