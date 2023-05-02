package com.github.dbmdz.flusswerk.framework.flow;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.exceptions.RetryProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.SkipProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.monitoring.Status;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("The FlowInfo")
class FlowInfoTest {

  record StatusFixture(Exception exception, Status expected) {}

  static Stream<StatusFixture> statusFixtures() {
    return Stream.of(
        new StatusFixture(new RetryProcessingException(""), Status.ERROR_RETRY),
        new StatusFixture(new SkipProcessingException(""), Status.SKIP),
        new StatusFixture(new StopProcessingException(""), Status.ERROR_STOP),
        new StatusFixture(new SkipProcessingException(""), Status.SKIP));
  }

  @DisplayName("should set the status from the exception")
  @ParameterizedTest
  @MethodSource("statusFixtures")
  void shouldSetTheStatusFromTheException(StatusFixture fixture) {
    var flowInfo = new FlowInfo(null);
    flowInfo.setStatusFrom(fixture.exception);
    assertThat(flowInfo.getStatus()).isEqualTo(fixture.expected);
  }

  @DisplayName("should return the duration")
  @Test
  void shouldReturnTheDuration() {
    var startTime = System.nanoTime();
    var flowInfo = new FlowInfo(null);
    var stopTime = System.nanoTime();
    flowInfo.stop();
    assertThat(flowInfo.duration())
        .isCloseTo(Duration.ofNanos(stopTime - startTime), Duration.ofMillis(1));
  }
}
