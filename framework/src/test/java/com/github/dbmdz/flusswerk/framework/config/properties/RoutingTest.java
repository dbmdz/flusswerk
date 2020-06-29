package com.github.dbmdz.flusswerk.framework.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The Routing config")
class RoutingTest {

  @Test
  @DisplayName("should be constructed with no FailurePolicyProperties")
  void shouldBeConstructedWithoutFailurePolicyProperties() {
    RoutingProperties routing =
        new RoutingProperties(
            "test.exchange", List.of("in"), Map.of("default", "out"), Collections.emptyMap());
    assertThat(routing.getFailurePolicy("in")).isNotNull();
  }
}
