package com.github.dbmdz.flusswerk.framework.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
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
    String stringRepresentation = StringRepresentation.of(FlusswerkProperties.class).toString();
    assertThat(stringRepresentation).startsWith("FlusswerkProperties");
  }

  @DisplayName("should intend properties")
  @Test
  void propertiesAreIntended() {
    String stringRepresentation =
        StringRepresentation.of(FlusswerkProperties.class)
            .property("host", "example.com")
            .property("username", "guest")
            .toString();

    assertThat(titleOf(stringRepresentation)).doesNotStartWith(INDENTATION);
    assertThat(linesWithPropertiesOf(stringRepresentation)).are(STARTING_WITH_INDENTATION);
  }

  @DisplayName("should intend multiline properties")
  @Test
  void multilinePropertiesAreIntended() {
    String stringRepresentation =
        StringRepresentation.of(FlusswerkProperties.class)
            .property("host", "example.com")
            .property("description", "abc\ndef")
            .toString();

    assertThat(titleOf(stringRepresentation)).doesNotStartWith(INDENTATION);
    assertThat(linesWithPropertiesOf(stringRepresentation)).are(STARTING_WITH_INDENTATION);
  }

  @DisplayName("should mask masked properties")
  @Test
  void maskedPropertiesAreMasked() {
    String stringRepresentation =
        StringRepresentation.of(FlusswerkProperties.class)
            .maskedProperty("secret", "very_secret")
            .toString();

    assertThat(getPropertyValue("secret", stringRepresentation)).isEqualTo("v*****");
  }

  @DisplayName("should display lists")
  @Test
  void shouldDisplayLists() {
    var expected = "FlusswerkProperties\n" + "\titems:\n" + "\t\t- a\n" + "\t\t- b\n";
    var actual =
        StringRepresentation.of(FlusswerkProperties.class)
            .property("items", List.of("a", "b"))
            .toString();
    assertThat(actual).isEqualTo(expected);
  }

  @DisplayName("should display maps")
  @Test
  void shouldDisplayMaps() {
    var expected = "FlusswerkProperties\n" + "\titems:\n" + "\t\ta:\tA\n" + "\t\tb:\tB\n";
    var actual =
        StringRepresentation.of(FlusswerkProperties.class)
            .property("items", Map.of("a", "A", "b", "B"))
            .toString();
    assertThat(actual).isEqualTo(expected);
  }

  private String titleOf(String text) {
    return text.split("\n")[0];
  }

  private List<String> linesWithPropertiesOf(String text) {
    var lines = List.of(text.split("\n"));
    return lines.subList(1, lines.size());
  }

  private String getPropertyValue(String property, String stringRepresentation) {
    for (String line : linesWithPropertiesOf(stringRepresentation)) {
      String[] kv = line.strip().split(":");
      if (property.equals(kv[0])) {
        return kv[1].strip();
      }
    }
    return null;
  }
}
