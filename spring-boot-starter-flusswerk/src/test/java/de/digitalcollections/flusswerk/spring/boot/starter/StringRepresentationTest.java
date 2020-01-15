package de.digitalcollections.flusswerk.spring.boot.starter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class StringRepresentationTest {

  private static final String INDENTATION = "\t";

  private static final Condition<String> STARTING_WITH_INDENTATION =
      new Condition<>(line -> line.startsWith(INDENTATION), "starting with indentation");

  @Test
  void shouldContainClassName() {
    String stringRepresentation = StringRepresentation.of(FlusswerkProperties.class).toString();
    assertThat(stringRepresentation).startsWith("FlusswerkProperties");
  }

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

  @Test
  void maskedPropertiesAreMasked() {
    String stringRepresentation =
        StringRepresentation.of(FlusswerkProperties.class)
            .maskedProperty("secret", "verysecret")
            .toString();

    assertThat(getPropertyValue("secret", stringRepresentation)).isEqualTo("v*****");
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
