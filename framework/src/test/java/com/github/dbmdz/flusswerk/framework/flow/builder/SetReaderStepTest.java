package com.github.dbmdz.flusswerk.framework.flow.builder;

import static org.assertj.core.api.Assertions.assertThat;

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

    step.reader(TestMessage::getId);

    var expected = "test";
    var actual = model.getReader().apply(new TestMessage(expected));
    assertThat(actual).isEqualTo(expected);
  }
}
