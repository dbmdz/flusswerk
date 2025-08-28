package dev.mdz.flusswerk.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.mdz.flusswerk.config.properties.ProcessingProperties;
import dev.mdz.flusswerk.flow.FlowInfo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The FlusswerkMetrics")
class FlusswerkMetricsTest {

  private static final int THREADS = 5;

  private static final FlowInfoFixture[] flowInfoFixtures =
      new FlowInfoFixture[] {
        new FlowInfoFixture(Status.SUCCESS, Duration.ofSeconds(5L)),
        new FlowInfoFixture(Status.ERROR_RETRY, Duration.ofSeconds(5L)),
        new FlowInfoFixture(Status.ERROR_STOP, Duration.ofSeconds(5L)),
        new FlowInfoFixture(Status.SKIP, Duration.ofSeconds(5L))
      };

  private FlusswerkMetrics flusswerkMetrics;
  private Gauge activeWorkers;
  private Gauge idleWorkers;

  private MeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    ProcessingProperties processingProperties = new ProcessingProperties(THREADS);
    flusswerkMetrics = new FlusswerkMetrics(processingProperties, meterRegistry);
    activeWorkers = meterRegistry.get("flusswerk.workers").tag("state", "active").gauge();
    idleWorkers = meterRegistry.get("flusswerk.workers").tag("state", "idle").gauge();
  }

  @DisplayName("should increment active workers")
  @Test
  void incrementActiveWorkers() {
    int expectedActiveWorkers = 3;
    int expectedIdleWorkers = THREADS - expectedActiveWorkers;

    for (int i = 0; i < expectedActiveWorkers; i++) {
      flusswerkMetrics.incrementActiveWorkers();
    }

    assertThat(activeWorkers.value()).isEqualTo(expectedActiveWorkers);
    assertThat(idleWorkers.value()).isEqualTo(expectedIdleWorkers);
  }

  @DisplayName("should decrement active workers")
  @Test
  void decrementActiveWorkers() {
    int expectedActiveWorkers = 3;
    int expectedIdleWorkers = THREADS - expectedActiveWorkers;

    for (int i = 0; i < THREADS; i++) {
      flusswerkMetrics.incrementActiveWorkers();
    }

    for (int i = 0; i < expectedIdleWorkers; i++) {
      flusswerkMetrics.decrementActiveWorkers();
    }

    assertThat(activeWorkers.value()).isEqualTo(expectedActiveWorkers);
    assertThat(idleWorkers.value()).isEqualTo(expectedIdleWorkers);
  }

  record FlowInfoFixture(Status status, Duration duration) {
    FlowInfo flowInfo() {
      FlowInfo flowInfo = mock(FlowInfo.class);
      when(flowInfo.getStatus()).thenReturn(status);
      when(flowInfo.duration()).thenReturn(duration);
      return flowInfo;
    }

    String tag() {
      return status.name().toLowerCase();
    }
  }

  @DisplayName("should measure message counts")
  @Test
  void shouldMeasureMessageCounts() {
    for (var flowInfoFixture : flowInfoFixtures) {
      flusswerkMetrics.accept(flowInfoFixture.flowInfo());
    }
    for (var flowInfoFixture : flowInfoFixtures) {
      double counts =
          meterRegistry
              .get("flusswerk.messages")
              .tag("status", flowInfoFixture.tag())
              .counter()
              .count();
      assertThat(counts).isEqualTo(1);
    }
    double totalCount =
        meterRegistry.get("flusswerk.messages").counters().stream()
            .map(Counter::count)
            .reduce(0.0, Double::sum);
    assertThat(totalCount).isEqualTo(flowInfoFixtures.length);
  }

  @DisplayName("should measure duration")
  @Test
  void shouldMeasureDuration() {
    for (var flowInfoFixture : flowInfoFixtures) {
      flusswerkMetrics.accept(flowInfoFixture.flowInfo());
    }
    for (var flowInfoFixture : flowInfoFixtures) {
      double duration =
          meterRegistry
              .get("flusswerk.messages.seconds")
              .tag("status", flowInfoFixture.tag())
              .counter()
              .count();
      assertThat(duration).isEqualTo(flowInfoFixture.duration().getSeconds());
    }
    double totalDuration =
        meterRegistry.get("flusswerk.messages.seconds").counters().stream()
            .map(Counter::count)
            .reduce(0.0, Double::sum);
    assertThat(totalDuration).isEqualTo(flowInfoFixtures.length * 5L);
  }
}
