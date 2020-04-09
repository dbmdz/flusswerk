package de.digitalcollections.flusswerk.engine.flow.builder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SetTransformerStepTest {

  private Model<TestMessage, String, String> model;
  private SetTransformerStep<TestMessage, String, String> step;

  @BeforeEach
  void setUp() {
    model = new Model<>();
    step = new SetTransformerStep<>(model);
  }

  @Test
  void transformer() {
    step.transformer(String::toUpperCase);

    var expected = "TEST";
    var actual = model.getTransformer().apply("test");

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void noTransformer() {
    step.noTransformer();
    assertThat(model.getTransformer()).isNull();
  }
}
