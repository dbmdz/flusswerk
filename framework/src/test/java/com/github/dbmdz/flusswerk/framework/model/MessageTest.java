package com.github.dbmdz.flusswerk.framework.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("The Message")
class MessageTest {

  private static Stream<Arguments> messages() {
    return Stream.of(
        Arguments.of(new Message("123"), new Message("123"), true),
        Arguments.of(new Message(), new Message(), true),
        Arguments.of(new Message("123"), new Message(), false),
        Arguments.of(new Message("123"), new Message("999"), false));
  }

  @DisplayName("should be equal to another message with the same tracing id")
  @ParameterizedTest
  @MethodSource("messages")
  void testEquals(Message m1, Message m2, boolean shouldBeEqual) {
    assertThat(m1.equals(m2)).isEqualTo(shouldBeEqual);
  }
}
