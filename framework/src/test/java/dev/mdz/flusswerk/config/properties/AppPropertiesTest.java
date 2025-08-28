package dev.mdz.flusswerk.config.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.mdz.flusswerk.config.FlusswerkPropertiesConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = FlusswerkPropertiesConfiguration.class)
public class AppPropertiesTest {

  @Autowired private AppProperties properties;

  @DisplayName("should have application name")
  @Test
  void shouldHaveAppNameFromApplicationYaml() {
    assertThat(properties.name()).isEqualTo("flusswerk.test");
  }

  @DisplayName("should never have empty application name")
  @ParameterizedTest(name = "name=\"{0}\"")
  @NullSource
  @ValueSource(strings = {"", " \t"})
  void shouldNeverHaveEmptyAppName(String name) {
    assertThatThrownBy(() -> new AppProperties(name))
        .hasMessageContaining("needs to define spring.application.name");
  }
}
