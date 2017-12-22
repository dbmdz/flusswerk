package de.digitalcollections.workflow.examples.spring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UppercaseTransformerTest {

  @Test
  @DisplayName("Apply should make all letters uppercase")
  void applyShouldMakeAllLetterUppercase() {
    UppercaseTransformer uppercaseTransformer = new UppercaseTransformer();
    assertThat(uppercaseTransformer.apply("Shibuyara")).isEqualTo("SHIBUYARA");
  }

  @Test
  @DisplayName("Apply should do nothing if the value is null")
  void applyShouldDoNothingForNull() {
    UppercaseTransformer uppercaseTransformer = new UppercaseTransformer();
    assertThat(uppercaseTransformer.apply(null)).isNull();
  }

}
