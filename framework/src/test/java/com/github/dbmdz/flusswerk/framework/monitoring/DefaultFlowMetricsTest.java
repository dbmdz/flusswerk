package com.github.dbmdz.flusswerk.framework.monitoring;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dbmdz.flusswerk.framework.flow.FlowInfo;
import io.micrometer.core.instrument.Counter;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("The DefaultFlowMetrics")
class DefaultFlowMetricsTest {

  private static Stream<Arguments> counters() {
    return Stream.concat(
        Arrays.stream(Status.values()).map(status -> Arguments.of("processed.items", status)),
        Arrays.stream(Status.values()).map(status -> Arguments.of("execution.time", status)));
  }

  @DisplayName("Should initialize counters")
  @ParameterizedTest
  @MethodSource("counters")
  void shouldInitializeCounters(String metric, Status status) {
    MeterFactory meterFactory = mock(MeterFactory.class);
    new DefaultFlowMetrics(meterFactory);
    verify(meterFactory).counter(metric, status);
  }

  @DisplayName("should use all fields of FlowInfo")
  @Test
  void shouldUseAllFieldsOfFlowInfo() {
    MeterFactory meterFactory = mock(MeterFactory.class);
    Counter counter = mock(Counter.class);
    when(meterFactory.counter(any(), eq(Status.SUCCESS))).thenReturn(counter);
    when(meterFactory.frameworkCounter(any(), eq(Status.SUCCESS))).thenReturn(counter);
    DefaultFlowMetrics defaultFlowMetrics = new DefaultFlowMetrics(meterFactory);

    FlowInfo flowInfo = mock(FlowInfo.class);
    when(flowInfo.getStatus()).thenReturn(Status.SUCCESS);
    defaultFlowMetrics.accept(flowInfo);
    verify(flowInfo, times(2)).duration();
    verify(flowInfo).getStatus();
  }
}
