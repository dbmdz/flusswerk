package de.digitalcollections.workflow.engine;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
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

  private Function<MessageBrokerConfig, MessageBrokerConnection> create_connection = config -> {
    try {
      return new MessageBrokerConnection(config, connectionFactory);
    } catch (IOException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  };

  @BeforeEach
  void setUp() throws IOException, TimeoutException {
    connectionFactory = mock(ConnectionFactory.class);
    connection = mock(Connection.class);
    when(connectionFactory.newConnection()).thenReturn(connection);
    channel = mock(Channel.class);
    when(connection.createChannel()).thenReturn(channel);
    create_connection = config -> {
      try {
        return new MessageBrokerConnection(config, connectionFactory);
      } catch (IOException | TimeoutException e) {
        throw new RuntimeException(e);
      }
    };
  }


  @Test
  @DisplayName("Default hostname is RabbitMQ default")
  void defaultHostName() throws IOException {
    messageBroker = new MessageBrokerBuilder().build(create_connection);
    verify(connectionFactory).setHost("localhost");
  }

  @Test
  @DisplayName("Should set hostname to desired value")
  void hostName() throws IOException {
    messageBroker = new MessageBrokerBuilder()
        .hostName("example.com")
        .build(create_connection);
    verify(connectionFactory).setHost("example.com");
  }

  @Test
  @DisplayName("Default password is RabbitMQ default")
  void defaultPassword() throws IOException {
    messageBroker = new MessageBrokerBuilder().build(create_connection);
    verify(connectionFactory).setPassword("guest");
  }


  @Test
  @DisplayName("Password should be set")
  void password() throws IOException {
    messageBroker = new MessageBrokerBuilder()
        .password("secretsecretsecret")
        .build(create_connection);
    verify(connectionFactory).setPassword("secretsecretsecret");
  }

  @Test
  @DisplayName("Default port is RabbitMQ default")
  void defaultPort() throws IOException {
    messageBroker = new MessageBrokerBuilder().build(create_connection);
    verify(connectionFactory).setPort(5672);
  }

  @Test
  @DisplayName("Should set port")
  void port() throws IOException {
    messageBroker = new MessageBrokerBuilder()
        .port(12345)
        .build(create_connection);
    verify(connectionFactory).setPort(12345);
  }

  @Test
  @DisplayName("Default username is RabbitMQ default")
  void defaultUsername() throws IOException {
    messageBroker = new MessageBrokerBuilder().build(create_connection);
    verify(connectionFactory).setUsername("guest");
  }

  @Test
  @DisplayName("Should set username")
  void username() throws IOException {
    messageBroker = new MessageBrokerBuilder()
        .username("Hackerman")
        .build(create_connection);
    verify(connectionFactory).setUsername("Hackerman");
  }

  @Test
  @DisplayName("Default virtualHost is RabbitMQ default")
  void defaultVirtualHost() throws IOException {
    messageBroker = new MessageBrokerBuilder().build(create_connection);
    verify(connectionFactory).setVirtualHost("/");
  }

  @Test
  @DisplayName("Default virtualHost is RabbitMQ default")
  void virtualHost() throws IOException {
    messageBroker = new MessageBrokerBuilder()
        .virtualHost("/special")
        .build(create_connection);
    verify(connectionFactory).setVirtualHost("/special");
  }

  @Test
  @DisplayName("Default dead letter backoff time is 30s")
  void defaultDeadLetterWait() throws IOException {
    messageBroker = new MessageBrokerBuilder().build(create_connection);
    assertThat(messageBroker.getDeadLetterWait()).isEqualTo(30 * 1000);
  }

  @Test
  @DisplayName("Default dead letter backoff time is 30s")
  void deadLetterWait() throws IOException {
    messageBroker = new MessageBrokerBuilder()
        .deadLetterWait(321)
        .build(create_connection);
    assertThat(messageBroker.getDeadLetterWait()).isEqualTo(321);
  }

  @Test
  @DisplayName("Default max retries is 5")
  void defaultMaxRetries() throws IOException {
    messageBroker = new MessageBrokerBuilder().build(create_connection);
    assertThat(messageBroker.getMaxRetries()).isEqualTo(5);
  }

  @Test
  @DisplayName("Default max retries is 5")
  void maxRetries() throws IOException {
    messageBroker = new MessageBrokerBuilder()
        .maxRetries(42)
        .build(create_connection);
    assertThat(messageBroker.getMaxRetries()).isEqualTo(42);
  }

  @Test
  @DisplayName("Default exchanges should be workflow and dlx.workflow")
  void defaultExchanges() throws IOException {
    messageBroker = new MessageBrokerBuilder().build(create_connection);
    verify(channel).exchangeDeclare(eq("workflow"), any(String.class), anyBoolean());
    verify(channel).exchangeDeclare(eq("dlx.workflow"), any(String.class), anyBoolean());
  }

  @Test
  @DisplayName("Default exchanges should be workflow and dlx.workflow")
  void exchanges() throws IOException {
    messageBroker = new MessageBrokerBuilder()
        .exchanges("another", "dlx.another")
        .build(create_connection);
    verify(channel).exchangeDeclare(eq("another"), any(String.class), anyBoolean());
    verify(channel).exchangeDeclare(eq("dlx.another"), any(String.class), anyBoolean());
  }

}
