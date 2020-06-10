package com.github.dbmdz.flusswerk.framework.spring.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.dbmdz.flusswerk.framework.config.properties.FlusswerkProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.Monitoring;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MeterFactoryTest {

  @Test
  void shouldUseMonitoringPrefix() {
    var monitoringPrefix = "test.prefix";
    var monitoringMetric = "test.metric";

    FlusswerkProperties flusswerkProperties = mock(FlusswerkProperties.class);
    when(flusswerkProperties.getMonitoring()).thenReturn(new Monitoring(monitoringPrefix));

    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    MeterFactory meterFactory = new MeterFactory(flusswerkProperties, "test_app", meterRegistry);

    Counter counter = meterFactory.counter(monitoringMetric);
    Search search = meterRegistry.find(monitoringPrefix + "." + monitoringMetric);
    assertThat(search.counter()).isEqualTo(counter);
  }
}