package com.github.dbmdz.flusswerk.framework.flow.builder;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.TestMessage;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    var actual = evaluateFlow(message);

    assertThat(actual).containsExactly(expected);
  }

  @Test
  @DisplayName("should add a process to convert one message to another")
  void shouldAddProcessToConvertOneMessageToAnother() {
    step.process(m -> new TestMessage(m.getValues().get(0)));

    var expected = new TestMessage("a");
    var message = new TestMessage("1", expected.getId());
    var actual = evaluateFlow(message);

    assertThat(actual).containsExactly(expected);
  }

  @Test
  @DisplayName("should consume message")
  void shouldConsumeMessage() {
    var probe = new InvocationProbe<TestMessage>();
    step.consume(probe);

    var message = new TestMessage("1");
    var actual = evaluateFlow(message);

    assertThat(actual).isEmpty();
    assertThat(probe.hasBeenInvoked()).isTrue();
  }

  String[] allIdsOf(TestMessage... messages) {
    return Stream.of(messages).map(TestMessage::getId).toArray(String[]::new);
  }

  private Collection<Message> evaluateFlow(TestMessage message) {
    return model
        .getReader()
        .andThen(model.getTransformer())
        .andThen(model.getWriter())
        .apply(message);
  }
}
