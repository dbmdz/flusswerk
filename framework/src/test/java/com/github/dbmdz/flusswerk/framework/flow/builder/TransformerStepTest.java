package com.github.dbmdz.flusswerk.framework.flow.builder;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.TestMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransformerStepTest {

  private Model<TestMessage, String, String> model;
  private TransformerStep<TestMessage, String, String> step;

  @BeforeEach
  void setUp() {
    model = new Model<>();
    step = new TransformerStep<>(model);
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
