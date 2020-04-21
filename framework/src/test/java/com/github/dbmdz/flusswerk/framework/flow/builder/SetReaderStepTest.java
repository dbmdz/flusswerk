package com.github.dbmdz.flusswerk.framework.flow.builder;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.model.FlusswerkMessage;
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
