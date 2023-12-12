package com.github.dbmdz.flusswerk.framework.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.dbmdz.flusswerk.framework.reporting.Tracing.CurrentThread;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("The Tracing")
class TracingTest {

  private Tracing tracing;

  private static Stream<Arguments> incomingTracingIds() {
    return Stream.of(
        arguments(null, Collections.emptyList()),
        arguments(Collections.emptyList(), Collections.emptyList()),
        arguments(List.of("A", "B", "C"), List.of("A", "B", "C")));
  }

  @BeforeEach
  void setUp() {
    CurrentThread threadSource = mock(CurrentThread.class);
    when(threadSource.id()).thenReturn(123123123L);
    tracing = new Tracing(threadSource);
  }

  @DisplayName("should deregister ids for current thread")
  @Test
  void deregister() {
    tracing.register(List.of("A", "B", "C"));
    tracing.deregister();
    assertThat(tracing.tracingPath()).isEmpty();
  }

  @DisplayName("should set expected paths")
  @ParameterizedTest
  @MethodSource("incomingTracingIds")
  void registerShouldSetExpectedValues(List<String> input, List<String> expected) {
    tracing.register(input);

    assertThat(tracing.tracingPath()).hasSize(expected.size() + 1);

    if (input != null && !input.isEmpty()) {
      assertThat(tracing.tracingPath()).startsWith(expected.toArray(new String[] {}));
    }

    tracing.deregister();
  }

  @DisplayName("should return empty list if nothing is registered")
  @Test
  void register() {
    assertThat(tracing.tracingPath()).isEmpty();
  }
}
