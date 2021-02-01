package com.github.dbmdz.flusswerk.framework.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The StringRepresentation")
class StringRepresentationTest {

  private static final String INDENTATION = "\t";

  private static final Condition<String> STARTING_WITH_INDENTATION =
      new Condition<>(line -> line.startsWith(INDENTATION), "starting with indentation");

  @DisplayName("should contain the class name")
  @Test
  void shouldContainClassName() {
    ProcessingProperties properties = new ProcessingProperties(123);
    String actual = StringRepresentation.of(properties);
    assertThat(actual).contains("ProcessingProperties");
  }

  @DisplayName("should contain property")
  @Test
  void shouldContainProperty() {
    ProcessingProperties properties = new ProcessingProperties(123);
    String actual = StringRepresentation.of(properties);
    assertThat(actual).contains("threads: 123");
  }
}
