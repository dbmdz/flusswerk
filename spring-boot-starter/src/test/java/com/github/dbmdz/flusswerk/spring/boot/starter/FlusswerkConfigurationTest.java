package com.github.dbmdz.flusswerk.spring.boot.starter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class FlusswerkConfigurationTest {

  @Test
  public void isSetShouldDetectNull() {
    assertThat(FlusswerkConfiguration.isSet(null)).isFalse();
  }

  @Test
  public void isSetShouldDetectEmptyString() {
    assertThat(FlusswerkConfiguration.isSet("")).isFalse();
  }

  @ParameterizedTest(name = "{index} value=\"{0}\"")
  @ValueSource(strings = {" ", "\\n", "  \\t\\n "})
  public void isSetShouldDetectWhitespace(String string) {
    // String replace for JUnit output
    String value = string.replace("\\n", "\n").replace("\\t", "\t");
    assertThat(FlusswerkConfiguration.isSet(value)).isFalse();
  }

  @Test
  public void isSetShouldDetectValue() {
    assertThat(FlusswerkConfiguration.isSet("name")).isTrue();
  }

  @Test
  public void isSetShouldAcceptNonStrings() {
    assertThat(FlusswerkConfiguration.isSet(42)).isTrue();
  }
}
