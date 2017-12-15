package de.digitalcollections.workflow.engine.messagebroker;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import de.digitalcollections.workflow.engine.CustomMessage;
import de.digitalcollections.workflow.engine.CustomMessageMixin;
import de.digitalcollections.workflow.engine.jackson.SingleClassModule;
import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;
import de.digitalcollections.workflow.engine.model.Meta;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitClientTest {

  private MessageBrokerConfigImpl config = new MessageBrokerConfigImpl();

  private MessageBrokerConnection connection;

  private Channel channel;

  private Message message;


  @BeforeEach
  void setUp() throws IOException, TimeoutException {
    connection = mock(MessageBrokerConnection.class);
    channel = mock(Channel.class);
    when(connection.getChannel()).thenReturn(channel);
    message = DefaultMessage.withType("Hey");
  }


  @Test
  void ack() throws IOException {
    RabbitClient rabbitClient = new RabbitClient(config, connection);
    message.getMeta().setDeliveryTag(123123123);
    rabbitClient.ack(message);
    verify(channel).basicAck(eq(message.getMeta().getDeliveryTag()), eq(false));
  }

  @Test
  void sendShouldUseCorrectRoutingKey() throws IOException {
    RabbitClient rabbitClient = new RabbitClient(config, connection);
    rabbitClient.send("workflow", "there", message);
    verify(channel).basicPublish(anyString(), eq("there"), any(), any(byte[].class));
  }

  @Test
  void shouldWorkWithCustomMessageType() throws IOException, TimeoutException {
    config.addJacksonModule(new SingleClassModule(CustomMessage.class, CustomMessageMixin.class));
    config.setMessageClass(CustomMessage.class);
    RabbitClient rabbitClient = new RabbitClient(config, connection);
    CustomMessage message = new CustomMessage();
    message.setCustomField("Blah!");
    Message recreated = rabbitClient.deserialize(new String(rabbitClient.serialize(message), StandardCharsets.UTF_8));
    assertThat(message.getCustomField()).isEqualTo(((CustomMessage) recreated).getCustomField());
  }


  @Test
  void receiveShouldPullTheInputQueue() throws IOException {
    RabbitClient rabbitClient = new RabbitClient(config, connection);

    long deliveryTag = 476253;
    String messageType = "test-message";
    String messageId = "123123123";
    int retries = 333;
    LocalDateTime timestamp = LocalDateTime.now();
    String inputQueue = "some.input.queue";


    Message<String> messageToReceive = createMessage(messageType, messageId, retries, timestamp);
    GetResponse response = createResponse(deliveryTag, messageToReceive, rabbitClient);

    String body = new String(response.getBody(), StandardCharsets.UTF_8);

    when(channel.basicGet(inputQueue, false)).thenReturn(response);
    Message message = rabbitClient.receive(inputQueue);
    assertThat(message.getMeta())
        .returns(body, from(Meta::getBody))
        .returns(deliveryTag, from(Meta::getDeliveryTag))
        .returns(retries, from(Meta::getRetries))
        .returns(timestamp, from(Meta::getTimestamp));
    assertThat(message)
        .returns(messageType, from(Message::getType))
        .returns(messageId, from(Message<String>::getId));
  }

  private Message<String> createMessage(String messageType, String messageId, int retries, LocalDateTime timestamp) throws IOException {
    Message<String> messageToReceive = DefaultMessage.withType(messageType).andId(messageId);
    messageToReceive.getMeta().setRetries(retries);
    messageToReceive.getMeta().setTimestamp(timestamp);
    return messageToReceive;
  }

  private GetResponse createResponse(long deliveryTag, Message<?> message, RabbitClient rabbitClient) throws IOException {
    Envelope envelope = new Envelope(deliveryTag, true, "workflow", "some.input.queue");
    BasicProperties basicProperties = new BasicProperties.Builder().build();
    return new GetResponse(
        envelope,
        basicProperties,
        rabbitClient.serialize(message),
        1
    );
  }

}
