package com.github.dbmdz.flusswerk.framework.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.dbmdz.flusswerk.framework.TestMessage;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("A Topic")
@ExtendWith(MockitoExtension.class)
class TopicTest {

  @Mock private RabbitClient rabbitClient;
  @Mock private Tracing tracing;

  private Topic topic;

  @BeforeEach
  void setUp() {
    topic = new Topic("test.topic", "text.exchange", rabbitClient, tracing);
  }

  @DisplayName("should send a single message")
  @Test
  void shouldSendOneMessage() throws IOException {
    var message = new TestMessage("123");
    topic.send(message);
    verify(rabbitClient).send(any(), any(), eq(message));
  }

  @DisplayName("should send all messages")
  @Test
  void shouldSendManyMessages() throws IOException {
    List<Message> messages = List.of(new TestMessage("1"), new TestMessage("2"));
    topic.send(messages);
    for (Message message : messages) {
      verify(rabbitClient).send(any(), any(), eq(message));
    }
  }

  @DisplayName("should be equal to another identical topic")
  @Test
  void testEquals() {
    var expected = new Topic(topic.getName(), "test.exchange", mock(RabbitClient.class), tracing);
    assertThat(topic).isEqualTo(expected);
  }

  @DisplayName("should have the same hash code as identical topic")
  @Test
  void testHashCode() {
    var expected = new Topic(topic.getName(), "test.exchange", mock(RabbitClient.class), tracing);
    assertThat(topic.hashCode()).isEqualTo(expected.hashCode());
  }

  @DisplayName("should have its name in String representation")
  @Test
  void testToString() {
    assertThat(topic.toString()).contains(topic.getName());
  }

  @DisplayName("should return its name")
  @Test
  void shouldGetName() {
    assertThat(topic.getName()).isEqualTo("test.topic");
  }
}
