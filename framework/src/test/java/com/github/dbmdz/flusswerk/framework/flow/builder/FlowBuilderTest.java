package com.github.dbmdz.flusswerk.framework.flow.builder;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.flow.Type;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The ExperimentalFlowBuilder")
class FlowBuilderTest {

  @Test
  @DisplayName("should build a regular flow (using classes)")
  void shouldBuildRegularFlowUsingClasses() {
    Flow flow =
        FlowBuilder.flow(Message.class, String.class, String.class)
            .reader(Message::getTracingId)
            .transformer(String::toUpperCase)
            .writerSendingMessage(Message::new)
            .build();
    assertThat(flow).isNotNull(); // Lame test, just a API demo for now
  }

  @Test
  @DisplayName("should build a regular flow (using types)")
  void shouldBuildRegularFlowUsingTypes() {
    Flow flow =
        FlowBuilder.flow(new Type<>() {}, new Type<String>() {}, new Type<String>() {})
            .reader(Message::getTracingId)
            .transformer(String::toUpperCase)
            .writerSendingMessage(Message::new)
            .build();
    assertThat(flow).isNotNull(); // Lame test, just a API demo for now
  }

  @Test
  @DisplayName("should build a message processing flow sending a single message (using class)")
  void shouldBuildMessageProcessingFlowReturningSingleMessage() {
    Flow flow =
        FlowBuilder.messageProcessor(Message.class)
            .process(message -> new Message(message.getTracingId()))
            .build();
    assertThat(flow).isNotNull(); // Lame test, just a API demo for now
  }

  @Test
  @DisplayName("should build a message processing flow sending a single message (using types)")
  void shouldBuildMessageProcessingFlowReturningSingleMessageUsingTypes() {
    Flow flow =
        FlowBuilder.messageProcessor(new Type<>() {})
            .process(message -> new Message(message.getTracingId()))
            .build();
    assertThat(flow).isNotNull(); // Lame test, just a API demo for now
  }

  @Test
  @DisplayName("should build a message processing flow sending a multiple messages (using class)")
  void shouldBuildMessageProcessingFlowReturningManyMessages() {
    Flow flow =
        FlowBuilder.messageProcessor(Message.class)
            .expand(message -> List.of(message, message, message))
            .build();
    assertThat(flow).isNotNull(); // Lame test, just a API demo for now
  }

  @Test
  @DisplayName("should build a message processing flow sending a multiple messages (using types)")
  void shouldBuildMessageProcessingFlowReturningManyMessagesUsingTypes() {
    Flow flow =
        FlowBuilder.messageProcessor(new Type<>() {})
            .expand(message -> List.of(message, message, message))
            .build();
    assertThat(flow).isNotNull(); // Lame test, just a API demo for now
  }
}
