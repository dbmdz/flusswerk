package de.digitalcollections.workflow.engine.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultMessageTest {

  @Test
  void putCanBeChained() {
    DefaultMessage message = new DefaultMessage("sundry").put("first", "is the first").put("second", "is the second");
    assertThat(message.get("first")).isEqualTo("is the first");
    assertThat(message.get("second")).isEqualTo("is the second");
  }

}
