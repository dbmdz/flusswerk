package de.digitalcollections.workflow.engine;


import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlowBuilderTest {

  @Test
  @DisplayName("read should throw an exception if the reader is null")
  public void readShouldThrowExceptionIfArgumentNull() {
    FlowBuilder<String, String> builder = new FlowBuilder<>();
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
      builder.read(null);
    });
    assertThat(exception.getMessage()).contains("reader");
  }

  @Test
  @DisplayName("transform should throw an exception if there is no reader")
  public void transformerShouldThrowExceptionIfReaderIsNull() {
    FlowBuilder<String, String> builder = new FlowBuilder<>();
    Throwable exception = assertThrows(IllegalStateException.class, () -> {
      builder.transform(s -> null);
    });
    assertThat(exception.getMessage()).contains("reader");
  }

  @Test
  @DisplayName("transform should throw an exception if transformer is null")
  public void transformerShouldThrowExceptionIfTransformerIsNull() {
    FlowBuilder<String, String> builder = new FlowBuilder<>();
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
      builder.read(message -> null);
      builder.transform(null);
    });
    assertThat(exception.getMessage()).contains("transformer");
  }

  @Test
  @DisplayName("build should create an working Flow if only reader and writer are set")
  public void buildWithOnlyReadAndWrite() {
    Flow<String, String> flow = new FlowBuilder<String, String>()
        .read(Message::getType)
        .write(DefaultMessage::withType)
        .build();
    String message = "Whiskey in the Jar";
    Message result = flow.process(DefaultMessage.withType(message));
    assertThat(result.getType()).isEqualTo(message);
  }

}
