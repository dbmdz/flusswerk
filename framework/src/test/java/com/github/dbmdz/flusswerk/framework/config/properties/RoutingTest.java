package com.github.dbmdz.flusswerk.framework.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The Routing config")
class RoutingTest {

  @Test
  @DisplayName("should be constructed with no FailurePolicyProperties")
  void shouldBeConstructedWithoutFailurePolicyProperties() {
    Routing routing = new Routing("test.exchange", List.of("in"), "out", Collections.emptyMap());
    assertThat(routing.getFailurePolicy("in")).isNotNull();
  }

}