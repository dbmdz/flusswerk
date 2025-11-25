package dev.mdz.flusswerk.integration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class IntegrationTestConfiguration {

  @Bean
  MeterRegistry meterRegistry() {
    return new SimpleMeterRegistry();
  }
}
