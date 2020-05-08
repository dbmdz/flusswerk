package com.github.dbmdz.flusswerk.framework.flow.builder;

import static com.github.dbmdz.flusswerk.framework.flow.builder.InvocationProbe.beenInvoked;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WriterStepTest {

  private Model<TestMessage, String, String> model;
  private WriterStep<TestMessage, String, String> step;

  @BeforeEach
  void setUp() {
    model = new Model<>();
    step = new WriterStep<>(model);
  }

  @Test
  void writerSendingNothing() {
    var consumingWriter = new InvocationProbe<String>();
    step.writerSendingNothing(consumingWriter);

    var actual = model.getWriter().apply("test");
    assertThat(consumingWriter).has(beenInvoked());
    Assertions.assertThat(actual).isEmpty();
  }

  @Test
  void writerSendingMessage() {
    step.writerSendingMessage(TestMessage::new);

    var expected = new TestMessage("test");
    var actual = model.getWriter().apply(expected.getId());
    Assertions.assertThat(actual).containsExactly(expected);
  }

  @Test
  void writerSendingMessages_2() {
    TestMessage[] expected = {new TestMessage("1"), new TestMessage("2"), new TestMessage("3")};
    step.writerSendingMessages(anything -> List.of(expected));

    var actual = model.getWriter().apply("123");
    Assertions.assertThat(actual).containsExactly(expected);
  }

  TestMessage[] messagesForIds(String... id) {
    return Stream.of(id).map(TestMessage::new).toArray(TestMessage[]::new);
  }
}
