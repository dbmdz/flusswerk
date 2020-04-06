package de.digitalcollections.flusswerk.engine.flow.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import de.digitalcollections.flusswerk.engine.flow.Flow;
import de.digitalcollections.flusswerk.engine.model.DefaultMessage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The ExperimentalFlowBuilder")
class ExperimentalFlowBuilderTest {

  @Test
  void shouldBuildAFlow() {
    Flow<DefaultMessage, String, String> flow =
        ExperimentalFlowBuilder.flow(DefaultMessage.class, String.class, String.class)
            .reader(DefaultMessage::getId)
            .transformer(String::toUpperCase)
            .writerSendingMessage(DefaultMessage::new)
            .build();
    assertThat(flow).isNotNull(); // Lame test, just a API demo for now
  }

  @Test
  void shouldBuildAMessageProcessingFlow() {
    Flow<DefaultMessage, DefaultMessage, DefaultMessage> flow =
        ExperimentalFlowBuilder.messageProcessor(DefaultMessage.class)
            .process(message -> List.of(message, message, message))
            .build();
    assertThat(flow).isNotNull(); // Lame test, just a API demo for now
  }
}
