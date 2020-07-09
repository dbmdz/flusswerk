package com.github.dbmdz.flusswerk.framework.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.config.properties.MonitoringProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MeterFactoryTest {

  @Test
  void shouldUseMonitoringPrefix() {
    var monitoringPrefix = "test.prefix";
    var monitoringMetric = "test.metric";

    MonitoringProperties monitoringProperties = new MonitoringProperties(monitoringPrefix);

    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    MeterFactory meterFactory = new MeterFactory(monitoringProperties, "test_app", meterRegistry);

    Counter counter = meterFactory.counter(monitoringMetric);
    Search search = meterRegistry.find(monitoringPrefix + "." + monitoringMetric);
    assertThat(search.counter()).isEqualTo(counter);
  }
}
