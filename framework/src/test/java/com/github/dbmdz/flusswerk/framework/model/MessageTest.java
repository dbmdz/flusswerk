package com.github.dbmdz.flusswerk.framework.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("The Message")
class MessageTest {

  private static Message messageWithTracing(String... tracing) {
    Message message = new Message();
    message.setTracing(List.of(tracing));
    return message;
  }

  private static Stream<Arguments> messages() {
    return Stream.of(
        Arguments.of(messageWithTracing("123"), messageWithTracing("123"), true),
        Arguments.of(messageWithTracing(), messageWithTracing(), true),
        Arguments.of(messageWithTracing("123"), messageWithTracing(), false),
        Arguments.of(messageWithTracing("123"), messageWithTracing("999"), false));
  }

  @DisplayName("should be equal to another message with the same tracing id")
  @ParameterizedTest
  @MethodSource("messages")
  void testEquals(Message m1, Message m2, boolean shouldBeEqual) {
    assertThat(m1.equals(m2)).isEqualTo(shouldBeEqual);
  }

  @DisplayName("should be have the same hashCode as another message with the same tracing id")
  @ParameterizedTest
  @MethodSource("messages")
  void testHashCode(Message m1, Message m2, boolean shouldBeEqual) {
    assertThat(m1.hashCode() == m2.hashCode()).isEqualTo(shouldBeEqual);
  }
}
