package com.github.dbmdz.flusswerk.framework.flow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The Flow")
class FlowTest {

  @DisplayName("should convert nanoseconds to seconds")
  @Test
  void shouldConvertNanosecondsToSeconds() {
    assertThat(Flow.ns_to_seconds(1000000000)).isEqualTo(1);
  }

  @DisplayName("should convert nanoseconds to milliseconds")
  @Test
  void shouldConvertNanosecondsToMilliseconds() {
    assertThat(Flow.ns_to_milliseconds(1000000000)).isEqualTo(1000);
  }
}
