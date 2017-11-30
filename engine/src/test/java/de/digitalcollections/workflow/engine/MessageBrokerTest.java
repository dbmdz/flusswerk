package de.digitalcollections.workflow.engine;

import com.rabbitmq.client.Channel;
import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageBrokerTest {

  private MessageBrokerConfig config = new MessageBrokerConfig();

  private MessageBrokerConnection connection;

  private Channel channel;

  private MessageBroker messageBroker;

  private Message message;

  @BeforeEach
  void setUp() throws IOException, TimeoutException {
    connection = mock(MessageBrokerConnection.class);
    channel = mock(Channel.class);
    when(connection.getChannel()).thenReturn(channel);
    messageBroker = new MessageBroker(config, connection);
    message = DefaultMessage.withType("Hey");
  }

  @Test
  void sendShouldUseCorrectRoutingKey() throws IOException {
    messageBroker.send("there", message);
    verify(channel).basicPublish(anyString(), eq("there"), any(), any(byte[].class));
  }

  @Test
  void ack() throws IOException {
    message.getMeta().setDeliveryTag(123123123);
    messageBroker.ack(message);
    verify(channel).basicAck(eq(message.getMeta().getDeliveryTag()), eq(false));
  }

  @Test
  void rejectShouldCountRejections() throws IOException {
    int numberOfRejections = 3;
    for (int i = 0; i < numberOfRejections; i++) {
      messageBroker.reject(message);
    }
    assertThat(message.getMeta().getRetries()).isEqualTo(numberOfRejections);
  }

  @Test
  void rejectShouldRouteToFailedQueueIfMessageIsRejectedTooOften() throws IOException {
    messageBroker.provideInputQueue("somequeue");
    int numberOfRejections = config.getMaxRetries() + 1;
    for (int i = 0; i < numberOfRejections; i++) {
      messageBroker.reject(message);
    }
    verify(channel).basicPublish(anyString(), eq("somequeue.failed"), any(), any());
  }

  @Test
  @Disabled("Need different test logic")
  void rejectShouldRemoveOriginalMessageIfMessageIsRejectedTooOften() throws IOException {
    messageBroker.provideInputQueue("somequeue");
    int numberOfRejections = config.getMaxRetries() + 1;
    for (int i = 0; i < numberOfRejections; i++) {
      messageBroker.reject(message);
    }
    verify(channel).basicAck(eq(message.getMeta().getDeliveryTag()), eq(false));
  }

  @Test
  void shouldWorkWithCustomMessageType() throws IOException, TimeoutException {
    config.setMessageMixin(CustomMessageMixin.class);
    config.setMessageClass(CustomMessage.class);
    messageBroker = new MessageBroker(config, connection);
    CustomMessage message = new CustomMessage();
    message.setCustomField("Blah!");
    Message recreated = messageBroker.deserialize(new String(messageBroker.serialize(message), StandardCharsets.UTF_8));
    assertThat(message.getCustomField()).isEqualTo(((CustomMessage) recreated).getCustomField());
  }

}
