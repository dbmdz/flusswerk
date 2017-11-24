package de.digitalcollections.workflow.engine;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageBrokerBuilderTest {

  private ConnectionFactory connectionFactory;

  private Channel channel;

  private Connection connection;

  private MessageBroker messageBroker;


  @BeforeEach
  void setUp() throws IOException, TimeoutException {
    connectionFactory = mock(ConnectionFactory.class);
    connection = mock(Connection.class);
    when(connectionFactory.newConnection()).thenReturn(connection);
    channel = mock(Channel.class);
    when(connection.createChannel()).thenReturn(channel);
    messageBroker = new MessageBrokerBuilder().build(config -> {
      try {
        return new MessageBrokerConnection(config, connectionFactory);
      } catch (IOException | TimeoutException e) {
        throw new RuntimeException(e);
      }
    });
  }


  @Test
  @DisplayName("Default hostname is RabbitMQ default")
  void hostName() throws IOException {
    verify(connectionFactory).setHost("localhost");
  }

  @Test
  @DisplayName("Default password is RabbitMQ default")
  void password() {
    verify(connectionFactory).setPassword("guest");
  }

  @Test
  @DisplayName("Default port is RabbitMQ default")
  void port() {
    verify(connectionFactory).setPort(5672);
  }

  @Test
  @DisplayName("Default username is RabbitMQ default")
  void username() {
    verify(connectionFactory).setUsername("guest");
  }

  @Test
  @DisplayName("Default virtualHost is RabbitMQ default")
  void virtualHost() {
    verify(connectionFactory).setVirtualHost("/");
  }

  @Test
  @DisplayName("Default dead letter backoff time is 30s")
  void deadLetterWait() {
    assertThat(messageBroker.getDeadLetterWait()).isEqualTo(30 * 1000);
  }

  @Test
  @DisplayName("Default max retries is 5")
  void maxRetries() {
    assertThat(messageBroker.getMaxRetries()).isEqualTo(5);
  }

  @Test
  @DisplayName("Default exchanges should be workflow and dlx.workflow")
  void defaultExchanges() throws IOException {
    verify(channel).exchangeDeclare(eq("workflow"), any(String.class), anyBoolean());
    verify(channel).exchangeDeclare(eq("dlx.workflow"), any(String.class), anyBoolean());
  }

}
