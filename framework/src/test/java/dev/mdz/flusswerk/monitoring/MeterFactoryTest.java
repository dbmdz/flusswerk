package dev.mdz.flusswerk.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MeterFactoryTest {

  @Test
  void shouldUseMonitoringPrefix() {
    var monitoringPrefix = "test.prefix";
    var monitoringMetric = "test.metric";

    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    MeterFactory meterFactory = new MeterFactory(monitoringPrefix, meterRegistry);

    Counter counter = meterFactory.counter(monitoringMetric);
    Search search = meterRegistry.find(monitoringPrefix + "." + monitoringMetric);
    assertThat(search.counter()).isEqualTo(counter);
  }
}
