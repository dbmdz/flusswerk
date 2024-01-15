package com.github.dbmdz.flusswerk.framework.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("RabbitMQ")
class RabbitMQTest {

  private static final List<String> incoming = List.of("first.incoming", "first.incoming");
  private static final Map<String, List<String>> outgoing =
      Map.of(
          "first.route",
          List.of("first.outgoing"),
          "second.route",
          List.of("second.outgoing", "another.outgoing"));

  private RabbitClient rabbitClient;
  private RabbitMQ rabbitMQ;
  private Tracing tracing;

  private static Stream<Arguments> routesAndTopics() {
    return outgoing.entrySet().stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
  }

  private static Stream<String> topics() {
    return Stream.concat(incoming.stream(), outgoing.values().stream().flatMap(List::stream));
  }

  private static Stream<String> queues() {
    return topics(); // Currently, we assume that all topics correspond to a queue and vice versa
  }

  @BeforeEach
  void setUp() {
    var routing = RoutingProperties.minimal(incoming, outgoing);
    rabbitClient = mock(RabbitClient.class);
    tracing = new Tracing();
    rabbitMQ = new RabbitMQ(routing, rabbitClient, mock(MessageBroker.class), tracing);
  }

  @DisplayName("should provide matching topics for routes")
  @ParameterizedTest
  @MethodSource("routesAndTopics")
  void shouldProvideMatchingTopicsForRoutes(String route, List<String> topics) {
    var expected =
        new Route(
            route,
            topics.stream()
                .map(topic -> new Topic(topic, mock(MessageBroker.class), tracing))
                .toList());
    assertThat(rabbitMQ.route(route)).isEqualTo(expected);
  }

  @DisplayName("should provide all topics")
  @ParameterizedTest
  @MethodSource("topics")
  void shouldProvideAllTopics(String name) {
    var expected = new Topic(name, mock(MessageBroker.class), tracing);
    assertThat(rabbitMQ.topic(name)).isEqualTo(expected);
  }

  @DisplayName("should provide unknown topic")
  @Test
  void shouldProvideUnknownTopic() {
    var expected = new Topic("some.queue", mock(MessageBroker.class), tracing);
    assertThat(rabbitMQ.topic("some.queue")).isEqualTo(expected);
  }

  @DisplayName("should provide all queues")
  @ParameterizedTest
  @MethodSource("queues")
  void shouldProvideAllQueues(String name) {
    var expected = new Queue(name, mock(RabbitClient.class));
    assertThat(rabbitMQ.queue(name)).isEqualTo(expected);
  }

  @DisplayName("should provide unknown queues")
  @Test
  void shouldProvideUnknownQueues() {
    var expected = new Queue("some.queue", mock(RabbitClient.class));
    assertThat(rabbitMQ.queue("some.queue")).isEqualTo(expected);
  }

  @DisplayName("should ack messages")
  @Test
  void ack() {
    Message message = new Message();
    message.getEnvelope().setDeliveryTag(123L);
    rabbitMQ.ack(message);
    verify(rabbitClient).ack(message.getEnvelope());
  }
}
