package com.github.dbmdz.flusswerk.framework.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.dbmdz.flusswerk.framework.TestMessage;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Route")
class RouteTest {
  private Route route;
  private List<Topic> topics;

  @BeforeEach
  void setUp() {
    topics = List.of(mock(Topic.class), mock(Topic.class));
    route = new Route("test.route", topics);
  }

  @DisplayName("should send a single message")
  @Test
  void shouldSendOneMessage() throws IOException {
    var message = new TestMessage("123");
    route.send(message);
    for (Topic topic : topics) {
      verify(topic).send(eq(message));
    }
  }

  @DisplayName("should send all messages")
  @Test
  void shouldSendManyMessages() throws IOException {
    List<Message> messages = List.of(new TestMessage("1"), new TestMessage("2"));
    route.send(messages);
    for (Topic topic : topics) {
      verify(topic).send(eq(messages));
    }
  }

  @DisplayName("should be equal to another identical route")
  @Test
  void testEquals() {
    var expected = new Route(route.getName(), topics);
    assertThat(route).isEqualTo(expected);
  }

  @DisplayName("should have the same hash code as identical topic")
  @Test
  void testHashCode() {
    var expected = new Route(route.getName(), topics);
    assertThat(route.hashCode()).isEqualTo(expected.hashCode());
  }

  @DisplayName("should have its name in String representation")
  @Test
  void testToString() {
    assertThat(route.toString()).contains(route.getName());
  }

  @DisplayName("should return its name")
  @Test
  void shouldGetName() {
    assertThat(route.getName()).isEqualTo("test.route");
  }
}
