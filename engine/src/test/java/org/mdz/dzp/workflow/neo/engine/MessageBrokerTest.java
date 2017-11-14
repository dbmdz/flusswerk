package org.mdz.dzp.workflow.neo.engine;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mdz.dzp.workflow.neo.engine.model.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageBrokerTest {

  private MessageBrokerConfig config = new MessageBrokerConfig();

  private Channel channel;

  private MessageBroker messageBroker;

  private Message message;

  @BeforeEach
  void setUp() throws IOException, TimeoutException {
    MessageBrokerConnection connection = mock(MessageBrokerConnection.class);
    channel = mock(Channel.class);
    when(connection.getChannel()).thenReturn(channel);
    messageBroker = new MessageBroker(config, connection);
    message = new Message("Hey");
  }

  @Test
  void sendShouldUseCorrectRoutingKey() throws IOException {
    messageBroker.send("there", message);
    verify(channel).basicPublish(anyString(), eq("there"), any(), any());
  }

  @Test
  void ack() throws IOException {
    message.setDeliveryTag(123123123);
    messageBroker.ack(message);
    verify(channel).basicAck(eq(message.getDeliveryTag()), eq(false));
  }

  @Test
  void rejectShouldCountRejections() throws IOException {
    int numberOfRejections = 3;
    for (int i=0; i < numberOfRejections; i++) {
      messageBroker.reject(message);
    }
    assertThat(message.getRetries()).isEqualTo(numberOfRejections);
  }

  @Test
  void rejectShouldRouteToFailedQueueIfMessageIsRejectedTooOften() throws IOException {
    messageBroker.provideInputQueue("somequeue");
    int numberOfRejections = config.getMaxRetries() + 1;
    for (int i=0; i < numberOfRejections; i++) {
      messageBroker.reject(message);
    }
    verify(channel).basicPublish(anyString(), eq("failed.somequeue"), any(), any());
  }

  @Test
  void rejectShouldRemoveOriginalMessageIfMessageIsRejectedTooOften() throws IOException {
    messageBroker.provideInputQueue("somequeue");
    int numberOfRejections = config.getMaxRetries() + 1;
    for (int i=0; i < numberOfRejections; i++) {
      messageBroker.reject(message);
    }
    verify(channel).basicAck(eq(message.getDeliveryTag()), eq(false));
  }

}
