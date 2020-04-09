package de.digitalcollections.flusswerk.engine.flow.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import de.digitalcollections.flusswerk.engine.model.FlusswerkMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The SetReaderStep")
class SetReaderStepTest {

  @BeforeEach
  void setUp() {}

  @DisplayName("should set a reader")
  @Test
  void shouldSetReader() {
    Model<TestMessage, String, String> model = new Model<>();
    SetReaderStep<TestMessage, String, String> step = new SetReaderStep<>(model);

    step.reader(FlusswerkMessage::getId);

    var expected = "test";
    var actual = model.getReader().apply(new TestMessage(expected));
    assertThat(actual).isEqualTo(expected);
  }
}
