package com.github.dbmdz.flusswerk.spring.boot.starter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("FlusswerkConfiguration should")
public class FlusswerkConfigurationTest {

  private static Stream<Arguments> isSetSource() {
    return Stream.of(
        Arguments.of("null", null, false),
        Arguments.of("Empty String", "", false),
        Arguments.of("Whitespace", "  \t ", false),
        Arguments.of("String value", "some value", true),
        Arguments.of("Other value", 42, true));
  }

  @ParameterizedTest(name = "{0} should be {2}")
  @DisplayName("detect if value is set")
  @MethodSource("isSetSource")
  void isSetShouldDetectIfValueIsSet(String label, Object value, boolean expected) {
    assertThat(FlusswerkConfiguration.isSet(value)).isEqualTo(expected);
  }
}
