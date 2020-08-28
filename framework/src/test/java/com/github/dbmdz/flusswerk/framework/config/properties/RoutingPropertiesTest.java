package com.github.dbmdz.flusswerk.framework.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The RoutingProperties")
class RoutingPropertiesTest {

  private RoutingProperties routingProperties;

  @BeforeEach
  void beforeEach() {
    routingProperties =
        new RoutingProperties(
            "default.exchange",
            List.of(
                "queue.with.default.exchange",
                "queue.with.specific.exchange",
                "queue.with.default.dlx",
                "queue.with.specific.dlx"),
            Collections.emptyMap(),
            Map.of("queue.with.specific.exchange", "specific.exchange"),
            Map.of("queue.with.specific.dlx", "specific.dlx"),
            Collections.emptyMap());
  }

  @DisplayName("should return default exchange if there no specific configuration")
  @Test
  void shouldReturnDefaultExchange() {
    assertThat(routingProperties.getExchange("queue.with.default.exchange"))
        .isEqualTo("default.exchange");
  }

  @DisplayName("should return specific exchange if defined")
  @Test
  void shouldReturnSpecificExchange() {
    assertThat(routingProperties.getExchange("queue.with.specific.exchange"))
        .isEqualTo("specific.exchange");
  }

  @DisplayName("should return default dead letter exchange if there no specific configuration")
  @Test
  void shouldReturnDefaultDlx() {
    assertThat(routingProperties.getDeadLetterExchange("queue.with.default.dlx"))
        .isEqualTo("default.exchange.retry");
  }

  @DisplayName("should return specific dead letter exchange if defined")
  @Test
  void shouldReturnSpecificDlx() {
    assertThat(routingProperties.getDeadLetterExchange("queue.with.specific.dlx"))
        .isEqualTo("specific.dlx");
  }
}
