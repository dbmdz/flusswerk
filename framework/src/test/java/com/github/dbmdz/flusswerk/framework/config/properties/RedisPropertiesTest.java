package com.github.dbmdz.flusswerk.framework.config.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("The RedisProperties")
class RedisPropertiesTest {

  @DisplayName("should allow not setting address")
  @ParameterizedTest(name = "name=\"{0}\"")
  @NullSource
  @ValueSource(strings = {"", " \t"})
  void shouldAllowNotSettingAddress(String address) {
    var properties = new RedisProperties(address, null);
    assertThat(properties.redisIsAvailable()).isFalse();
  }

  @DisplayName("should set default port if port is missing")
  @ParameterizedTest(name = "{0} → {1}")
  @CsvSource({"host,host:6379", "host:12345,host:12345"})
  void shouldSetDefaultPortIfPortIsMissing(String address, String expected) {
    var properties = new RedisProperties(address, null);
    assertThat(properties.getAddress()).isEqualTo(expected);
  }

  @DisplayName("should allow not setting password")
  @Test
  void shouldAllowNotSettingPassword() {
    var properties = new RedisProperties("localhost", null);
    assertThat(properties.getPassword()).isNull();
  }
}
