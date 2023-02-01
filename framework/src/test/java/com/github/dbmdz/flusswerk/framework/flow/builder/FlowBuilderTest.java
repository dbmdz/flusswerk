package com.github.dbmdz.flusswerk.framework.flow.builder;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.TestMessage;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.flow.Type;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The ExperimentalFlowBuilder")
class FlowBuilderTest {

  @Test
  @DisplayName("should build a regular flow (using classes)")
  void shouldBuildRegularFlowUsingClasses() {
    FlowSpec flow =
        FlowBuilder.flow(TestMessage.class, String.class, String.class)
            .reader(TestMessage::getId)
            .transformer(String::toUpperCase)
            .writerSendingMessage(TestMessage::new)
            .build();
    assertThat(flow.reader().apply(new TestMessage("abc"))).isEqualTo("abc");
    assertThat(flow.transformer().apply("abc")).isEqualTo("ABC");
    assertThat(flow.writer().apply("abc")).containsExactly(new TestMessage("abc"));
  }

  @Test
  @DisplayName("should build a regular flow (using types)")
  void shouldBuildRegularFlowUsingTypes() {
    FlowSpec flow =
        FlowBuilder.flow(new Type<TestMessage>() {}, new Type<String>() {}, new Type<String>() {})
            .reader(TestMessage::getId)
            .transformer(String::toUpperCase)
            .writerSendingMessage(TestMessage::new)
            .build();
    assertThat(flow.reader().apply(new TestMessage("abc"))).isEqualTo("abc");
    assertThat(flow.transformer().apply("abc")).isEqualTo("ABC");
    assertThat(flow.writer().apply("abc")).containsExactly(new TestMessage("abc"));
  }

  @Test
  @DisplayName("should build a message processing flow sending a single message (using class)")
  void shouldBuildMessageProcessingFlowReturningSingleMessage() {
    FlowSpec flow =
        FlowBuilder.messageProcessor(TestMessage.class)
            .process(message -> new TestMessage(message.getId().toUpperCase(Locale.GERMAN)))
            .build();
    assertThat(flow.writer().apply(new TestMessage("abc"))).containsExactly(new TestMessage("ABC"));
  }

  @Test
  @DisplayName("should build a message processing flow sending a single message (using types)")
  void shouldBuildMessageProcessingFlowReturningSingleMessageUsingTypes() {
    FlowSpec flow =
        FlowBuilder.messageProcessor(new Type<TestMessage>() {})
            .process(message -> new TestMessage(message.getId().toUpperCase(Locale.GERMAN)))
            .build();
    assertThat(flow.writer().apply(new TestMessage("abc"))).containsExactly(new TestMessage("ABC"));
  }

  @Test
  @DisplayName("should build a message processing flow sending a multiple messages (using class)")
  void shouldBuildMessageProcessingFlowReturningManyMessages() {
    FlowSpec flow =
        FlowBuilder.messageProcessor(Message.class)
            .expand(message -> List.of(message, message, message))
            .build();
    var message = new TestMessage("abc");
    assertThat(flow.writer().apply(message)).containsExactly(message, message, message);
  }

  @Test
  @DisplayName("should build a message processing flow sending a multiple messages (using types)")
  void shouldBuildMessageProcessingFlowReturningManyMessagesUsingTypes() {
    FlowSpec flow =
        FlowBuilder.messageProcessor(new Type<>() {})
            .expand(message -> List.of(message, message, message))
            .build();
    var message = new TestMessage("abc");
    assertThat(flow.writer().apply(message)).containsExactly(message, message, message);
  }
}
