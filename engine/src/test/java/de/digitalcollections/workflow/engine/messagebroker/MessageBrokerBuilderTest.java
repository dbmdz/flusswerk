package de.digitalcollections.workflow.engine.messagebroker;

import com.rabbitmq.client.BuiltinExchangeType;
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

  private MessageBroker messageBroker;

  private final Function<ConnectionConfig, RabbitConnection> create_connection = config -> {
    try {
      return new RabbitConnection(config, connectionFactory);
    } catch (IOException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  };

  private MessageBrokerBuilder messageBrokerBuilder;

  @BeforeEach
  void setUp() throws IOException, TimeoutException {
    connectionFactory = mock(ConnectionFactory.class);
    Connection connection = mock(Connection.class);
    when(connectionFactory.newConnection()).thenReturn(connection);
    channel = mock(Channel.class);
    when(connection.createChannel()).thenReturn(channel);
    messageBrokerBuilder = new MessageBrokerBuilder().readFrom("default");
  }


  @Test
  @DisplayName("Default hostname is RabbitMQ default")
  void defaultHostName() throws IOException {
    messageBroker = messageBrokerBuilder.build(create_connection);
    verify(connectionFactory).setHost("localhost");
  }

  @Test
  @DisplayName("Should set hostname to desired value")
  void hostName() throws IOException {
    messageBroker = messageBrokerBuilder
        .hostName("example.com")
        .build(create_connection);
    verify(connectionFactory).setHost("example.com");
  }

  @Test
  @DisplayName("Default password is RabbitMQ default")
  void defaultPassword() throws IOException {
    messageBroker = messageBrokerBuilder.build(create_connection);
    verify(connectionFactory).setPassword("guest");
  }


  @Test
  @DisplayName("Password should be set")
  void password() throws IOException {
    messageBroker = messageBrokerBuilder
        .password("secretsecretsecret")
        .build(create_connection);
    verify(connectionFactory).setPassword("secretsecretsecret");
  }

  @Test
  @DisplayName("Default port is RabbitMQ default")
  void defaultPort() throws IOException {
    messageBroker = messageBrokerBuilder.build(create_connection);
    verify(connectionFactory).setPort(5672);
  }

  @Test
  @DisplayName("Should set port")
  void port() throws IOException {
    messageBroker = messageBrokerBuilder
        .port(12345)
        .build(create_connection);
    verify(connectionFactory).setPort(12345);
  }

  @Test
  @DisplayName("Default username is RabbitMQ default")
  void defaultUsername() throws IOException {
    messageBroker = messageBrokerBuilder.build(create_connection);
    verify(connectionFactory).setUsername("guest");
  }

  @Test
  @DisplayName("Should set username")
  void username() throws IOException {
    messageBroker = messageBrokerBuilder
        .username("Hackerman")
        .build(create_connection);
    verify(connectionFactory).setUsername("Hackerman");
  }

  @Test
  @DisplayName("Default virtualHost is RabbitMQ default")
  void defaultVirtualHost() throws IOException {
    messageBroker = messageBrokerBuilder.build(create_connection);
    verify(connectionFactory).setVirtualHost("/");
  }

  @Test
  @DisplayName("Default virtualHost is RabbitMQ default")
  void virtualHost() throws IOException {
    messageBroker = messageBrokerBuilder
        .virtualHost("/special")
        .build(create_connection);
    verify(connectionFactory).setVirtualHost("/special");
  }

  @Test
  @DisplayName("Default dead letter backoff time is 30s")
  void defaultDeadLetterWait() throws IOException {
    messageBroker = messageBrokerBuilder.build(create_connection);
    assertThat(messageBroker.getConfig().getDeadLetterWait()).isEqualTo(30 * 1000);
  }

  @Test
  @DisplayName("Default dead letter backoff time is 30s")
  void deadLetterWait() throws IOException {
    messageBroker = messageBrokerBuilder
        .deadLetterWait(321)
        .build(create_connection);
    assertThat(messageBroker.getConfig().getDeadLetterWait()).isEqualTo(321);
  }

  @Test
  @DisplayName("Default max retries is 5")
  void defaultMaxRetries() throws IOException {
    messageBroker = messageBrokerBuilder.build(create_connection);
    assertThat(messageBroker.getConfig().getMaxRetries()).isEqualTo(5);
  }

  @Test
  @DisplayName("Default max retries is 5")
  void maxRetries() throws IOException {
    messageBroker = messageBrokerBuilder
        .maxRetries(42)
        .build(create_connection);
    assertThat(messageBroker.getConfig().getMaxRetries()).isEqualTo(42);
  }

  @Test
  @DisplayName("Default exchanges should be workflow and workflow.retry")
  void defaultExchanges() throws IOException {
    messageBroker = messageBrokerBuilder.build(create_connection);
    verify(channel).exchangeDeclare(eq("workflow"), any(BuiltinExchangeType.class), anyBoolean());
    verify(channel).exchangeDeclare(eq("workflow.retry"), any(BuiltinExchangeType.class), anyBoolean());
  }

  @Test
  @DisplayName("Default exchanges should be workflow and workflow.retry")
  void exchanges() throws IOException {
    messageBroker = messageBrokerBuilder
        .exchange("another")
        .deadLetterExchange("another.retry")
        .build(create_connection);
    verify(channel).exchangeDeclare(eq("another"), any(BuiltinExchangeType.class), anyBoolean());
    verify(channel).exchangeDeclare(eq("another.retry"), any(BuiltinExchangeType.class), anyBoolean());
  }

}
