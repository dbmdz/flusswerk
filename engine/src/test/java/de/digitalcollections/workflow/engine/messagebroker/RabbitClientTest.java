package de.digitalcollections.workflow.engine.messagebroker;

import com.rabbitmq.client.Channel;
import de.digitalcollections.workflow.engine.CustomMessage;
import de.digitalcollections.workflow.engine.CustomMessageMixin;
import de.digitalcollections.workflow.engine.messagebroker.MessageBrokerConfig;
import de.digitalcollections.workflow.engine.messagebroker.MessageBrokerConnection;
import de.digitalcollections.workflow.engine.messagebroker.RabbitClient;
import de.digitalcollections.workflow.engine.model.Message;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RabbitClientTest {

  private MessageBrokerConfig config = new MessageBrokerConfig();

  private MessageBrokerConnection connection;

  private Channel channel;

  @BeforeEach
  void setUp() throws IOException, TimeoutException {
    connection = mock(MessageBrokerConnection.class);
    channel = mock(Channel.class);
    when(connection.getChannel()).thenReturn(channel);
  }

  @Test
  void shouldWorkWithCustomMessageType() throws IOException, TimeoutException {
    config.setMessageMixin(CustomMessageMixin.class);
    config.setMessageClass(CustomMessage.class);
    RabbitClient rabbitClient = new RabbitClient(config, connection);
    CustomMessage message = new CustomMessage();
    message.setCustomField("Blah!");
    Message recreated = rabbitClient.deserialize(new String(rabbitClient.serialize(message), StandardCharsets.UTF_8));
    assertThat(message.getCustomField()).isEqualTo(((CustomMessage) recreated).getCustomField());
  }

}
