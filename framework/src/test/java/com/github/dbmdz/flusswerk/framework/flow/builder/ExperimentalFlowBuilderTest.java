package com.github.dbmdz.flusswerk.framework.flow.builder;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.flow.Type;
import com.github.dbmdz.flusswerk.framework.model.DefaultMessage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The ExperimentalFlowBuilder")
class ExperimentalFlowBuilderTest {

  @Test
  @DisplayName("should build a regular flow (using classes)")
  void shouldBuildRegularFlowUsingClasses() {
    Flow<DefaultMessage, String, String> flow =
        ExperimentalFlowBuilder.flow(DefaultMessage.class, String.class, String.class)
            .reader(DefaultMessage::getId)
            .transformer(String::toUpperCase)
            .writerSendingMessage(DefaultMessage::new)
            .build();
    assertThat(flow).isNotNull(); // Lame test, just a API demo for now
  }

  @Test
  @DisplayName("should build a regular flow (using types)")
  void shouldBuildRegularFlowUsingTypes() {
    Flow<DefaultMessage, String, String> flow =
        ExperimentalFlowBuilder.flow(
                new Type<DefaultMessage>() {}, new Type<String>() {}, new Type<String>() {})
            .reader(DefaultMessage::getId)
            .transformer(String::toUpperCase)
            .writerSendingMessage(DefaultMessage::new)
            .build();
    assertThat(flow).isNotNull(); // Lame test, just a API demo for now
  }

  @Test
  @DisplayName("should build a message processing flow sending a single message (using class)")
  void shouldBuildMessageProcessingFlowReturningSingleMessage() {
    Flow<DefaultMessage, DefaultMessage, DefaultMessage> flow =
        ExperimentalFlowBuilder.messageProcessor(DefaultMessage.class)
            .process(message -> new DefaultMessage(message.getId()))
            .build();
    assertThat(flow).isNotNull(); // Lame test, just a API demo for now
  }

  @Test
  @DisplayName("should build a message processing flow sending a single message (using types)")
  void shouldBuildMessageProcessingFlowReturningSingleMessageUsingTypes() {
    Flow<DefaultMessage, DefaultMessage, DefaultMessage> flow =
        ExperimentalFlowBuilder.messageProcessor(new Type<DefaultMessage>() {})
            .process(message -> new DefaultMessage(message.getId()))
            .build();
    assertThat(flow).isNotNull(); // Lame test, just a API demo for now
  }

  @Test
  @DisplayName("should build a message processing flow sending a multiple messages (using class)")
  void shouldBuildMessageProcessingFlowReturningManyMessages() {
    Flow<DefaultMessage, DefaultMessage, DefaultMessage> flow =
        ExperimentalFlowBuilder.messageProcessor(DefaultMessage.class)
            .expand(message -> List.of(message, message, message))
            .build();
    assertThat(flow).isNotNull(); // Lame test, just a API demo for now
  }

  @Test
  @DisplayName("should build a message processing flow sending a multiple messages (using types)")
  void shouldBuildMessageProcessingFlowReturningManyMessagesUsingTypes() {
    Flow<DefaultMessage, DefaultMessage, DefaultMessage> flow =
        ExperimentalFlowBuilder.messageProcessor(new Type<DefaultMessage>() {})
            .expand(message -> List.of(message, message, message))
            .build();
    assertThat(flow).isNotNull(); // Lame test, just a API demo for now
  }
}
