package com.github.dbmdz.flusswerk.framework.flow.builder;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The SetMessageProcessorStep")
class MessageProcessorStepTest {

  private Model<TestMessage, TestMessage, TestMessage> model;
  private MessageProcessorStep<TestMessage> step;

  @BeforeEach
  void setUp() {
    model = new Model<>();
    step = new MessageProcessorStep<>(model);
  }

  @Test
  @DisplayName("should add a process to expand one message to many")
  void shouldAddProcessToConvertOneMessageToMany() {

    step.expand(message -> message.values().map(TestMessage::new).collect(Collectors.toList()));

    TestMessage[] expected = {new TestMessage("a"), new TestMessage("b"), new TestMessage("c")};

    var message = new TestMessage("1", allIdsOf(expected));
    var actual =
        model.getReader().andThen(model.getTransformer()).andThen(model.getWriter()).apply(message);

    Assertions.assertThat(actual).containsExactly(expected);
  }

  @Test
  @DisplayName("should add a process to convert one message to another")
  void shouldAddProcessToConvertOneMessageToAnother() {
    step.process(m -> new TestMessage(m.getValues().get(0)));

    var expected = new TestMessage("a");
    var message = new TestMessage("1", expected.getId());
    var actual =
        model.getReader().andThen(model.getTransformer()).andThen(model.getWriter()).apply(message);

    Assertions.assertThat(actual).containsExactly(expected);
  }

  String[] allIdsOf(TestMessage... messages) {
    return Stream.of(messages).map(TestMessage::getId).toArray(String[]::new);
  }
}
