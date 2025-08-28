package dev.mdz.flusswerk.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("The RabbitMQ properties")
class RabbitMQPropertiesTest {

  private static Stream<Arguments> defaultHosts() {
    return Stream.of(Arguments.of((Object) null), Arguments.of(Collections.emptyList()));
  }

  @DisplayName("should default to localhost")
  @ParameterizedTest
  @MethodSource("defaultHosts")
  void shouldDefaultToLocalhost(List<String> hosts) {
    var properties = new RabbitMQProperties(hosts, null, null, null);
    assertThat(properties.hosts()).containsExactly("localhost");
  }
}
