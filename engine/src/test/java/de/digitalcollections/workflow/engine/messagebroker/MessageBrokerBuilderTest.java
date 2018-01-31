package de.digitalcollections.workflow.engine.messagebroker;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

  private MessageBroker messageBroker;

  private MessageBrokerBuilder messageBrokerBuilder;

  @BeforeEach
  void setUp() throws IOException, TimeoutException {
    connectionFactory = mock(ConnectionFactory.class);
    Connection connection = mock(Connection.class);
    when(connectionFactory.newConnection(any(List.class))).thenReturn(connection);
    channel = mock(Channel.class);
    when(connection.createChannel()).thenReturn(channel);
    messageBrokerBuilder = new MessageBrokerBuilder().readFrom("default");
  }

  private RabbitConnection rabbitConnection() {
    try {
      return new RabbitConnection(messageBrokerBuilder.getConnectionConfig(), connectionFactory);
    } catch (IOException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @DisplayName("Should add new address to desired value")
  void hostNameAndPort() throws IOException, TimeoutException {
    messageBroker = messageBrokerBuilder
        .connectTo("example.com", 1234)
        .build(rabbitConnection());
    verify(connectionFactory).newConnection(Collections.singletonList(new Address("example.com", 1234)));
  }

  @Test
  @DisplayName("Should add new addresses to desired values, even with different separators")
  void hostConnectionStrings() throws IOException, TimeoutException {
    messageBroker = messageBrokerBuilder
        .connectTo("rabbit1.example.com:1234,rabbit2.example.com:2345;rabbit3.example.org:3456")
        .build(rabbitConnection());
    List<Address> expectedAddresses = new ArrayList<>();
    expectedAddresses.add(new Address("rabbit1.example.com", 1234));
    expectedAddresses.add(new Address("rabbit2.example.com", 2345));
    expectedAddresses.add(new Address("rabbit3.example.org", 3456));
    verify(connectionFactory).newConnection(expectedAddresses);
  }

  @Test
  @DisplayName("Default password is RabbitMQ default")
  void defaultPassword() throws IOException {
    messageBroker = messageBrokerBuilder.build(rabbitConnection());
    verify(connectionFactory).setPassword("guest");
  }


  @Test
  @DisplayName("Password should be set")
  void password() throws IOException, TimeoutException {
    messageBroker = messageBrokerBuilder
        .password("secretsecretsecret")
        .build(rabbitConnection());
    verify(connectionFactory).setPassword("secretsecretsecret");
  }

  @Test
  @DisplayName("Default port and hostname are RabbitMQ default")
  void defaulAddress() throws IOException, TimeoutException {
    messageBroker = messageBrokerBuilder.build(rabbitConnection());
    verify(connectionFactory).newConnection(Collections.singletonList(new Address("localhost", 5672)));
  }

  @Test
  @DisplayName("Default username is RabbitMQ default")
  void defaultUsername() throws IOException {
    messageBroker = messageBrokerBuilder.build(rabbitConnection());
    verify(connectionFactory).setUsername("guest");
  }

  @Test
  @DisplayName("Should set username")
  void username() throws IOException, TimeoutException {
    messageBroker = messageBrokerBuilder
        .username("Hackerman")
        .build(new RabbitConnection(messageBrokerBuilder.getConnectionConfig(), connectionFactory));
    verify(connectionFactory).setUsername("Hackerman");
  }

  @Test
  @DisplayName("Default virtualHost is RabbitMQ default")
  void defaultVirtualHost() throws IOException {
    messageBroker = messageBrokerBuilder.build(rabbitConnection());
    verify(connectionFactory).setVirtualHost("/");
  }

  @Test
  @DisplayName("Default virtualHost is RabbitMQ default")
  void virtualHost() throws IOException, TimeoutException {
    messageBroker = messageBrokerBuilder
        .virtualHost("/special")
        .build(new RabbitConnection(messageBrokerBuilder.getConnectionConfig(), connectionFactory));
    verify(connectionFactory).setVirtualHost("/special");
  }

  @Test
  @DisplayName("Default dead letter backoff time is 30s")
  void defaultDeadLetterWait() throws IOException {
    messageBroker = messageBrokerBuilder.build(rabbitConnection());
    assertThat(messageBroker.getConfig().getDeadLetterWait()).isEqualTo(30 * 1000);
  }

  @Test
  @DisplayName("Default dead letter backoff time is 30s")
  void deadLetterWait() throws IOException {
    messageBroker = messageBrokerBuilder
        .deadLetterWait(321)
        .build(rabbitConnection());
    assertThat(messageBroker.getConfig().getDeadLetterWait()).isEqualTo(321);
  }

  @Test
  @DisplayName("Default max retries is 5")
  void defaultMaxRetries() throws IOException {
    messageBroker = messageBrokerBuilder.build(rabbitConnection());
    assertThat(messageBroker.getConfig().getMaxRetries()).isEqualTo(5);
  }

  @Test
  @DisplayName("Default max retries is 5")
  void maxRetries() throws IOException {
    messageBroker = messageBrokerBuilder
        .maxRetries(42)
        .build(rabbitConnection());
    assertThat(messageBroker.getConfig().getMaxRetries()).isEqualTo(42);
  }

  @Test
  @DisplayName("Default exchanges should be workflow and workflow.retry")
  void defaultExchanges() throws IOException {
    messageBroker = messageBrokerBuilder.build(rabbitConnection());
    verify(channel).exchangeDeclare(eq("workflow"), any(BuiltinExchangeType.class), anyBoolean());
    verify(channel).exchangeDeclare(eq("workflow.retry"), any(BuiltinExchangeType.class), anyBoolean());
  }

  @Test
  @DisplayName("Default exchanges should be workflow and workflow.retry")
  void exchanges() throws IOException {
    messageBroker = messageBrokerBuilder
        .exchange("another")
        .deadLetterExchange("another.retry")
        .build(rabbitConnection());
    verify(channel).exchangeDeclare(eq("another"), any(BuiltinExchangeType.class), anyBoolean());
    verify(channel).exchangeDeclare(eq("another.retry"), any(BuiltinExchangeType.class), anyBoolean());
  }

}
