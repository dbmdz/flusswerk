package de.digitalcollections.workflow.engine.flow;


import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlowBuilderTest {

  @Test
  @DisplayName("read should throw an exception if the reader is null")
  public void readShouldThrowExceptionIfReaderIsNull() {
    FlowBuilder<Message, String, String> builder = new FlowBuilder<>();
    Throwable exception = assertThrows(NullPointerException.class, () -> builder.read((Function<Message, String>) null));
    assertThat(exception.getMessage()).contains("reader");
  }

  @Test
  @DisplayName("read should throw an exception if the reader factory is null")
  public void readShouldThrowExceptionIfFactoryIsNull() {
    FlowBuilder<Message, String, String> builder = new FlowBuilder<>();
    Throwable exception = assertThrows(NullPointerException.class, () -> builder.read((Supplier<Function<Message, String>>) null));
    assertThat(exception.getMessage()).contains("reader factory");
  }

  @Test
  @DisplayName("transform should throw an exception if there is no reader")
  public void transformerShouldThrowExceptionIfReaderIsNull() {
    FlowBuilder<Message, String, String> builder = new FlowBuilder<>();
    Throwable exception = assertThrows(IllegalStateException.class, () -> builder.transform(s -> null));
    assertThat(exception.getMessage()).contains("reader");
  }

  @Test
  @DisplayName("transform should throw an exception if transformer is null")
  public void transformShouldThrowExceptionIfTransformerIsNull() {
    FlowBuilder<Message, String, String> builder = new FlowBuilder<>();
    Throwable exception = assertThrows(NullPointerException.class, () -> {
      builder.read(message -> null);
      builder.transform((Function<String, String>) null);
    });
    assertThat(exception.getMessage()).contains("transformer");
  }

  @Test
  @DisplayName("transform should throw an exception if transformer factory is null")
  public void transformerShouldThrowExceptionIfTransformerFactoryIsNull() {
    FlowBuilder<Message, String, String> builder = new FlowBuilder<>();
    Throwable exception = assertThrows(NullPointerException.class, () -> {
      builder.read(message -> null);
      builder.transform((Supplier<Function<String, String>>) null);
    });
    assertThat(exception.getMessage()).contains("transformer factory");
  }

  @Test
  @DisplayName("write should throw an exception if writer is null")
  public void writeShouldThrowExceptionIfWriterIsNull() {
    FlowBuilder<Message, String, String> builder = new FlowBuilder<>();
    Throwable exception = assertThrows(NullPointerException.class, () -> {
      builder.read(message -> null);
      builder.write((Function<String, Message>) null);
    });
    assertThat(exception.getMessage()).contains("writer");
  }

  @Test
  @DisplayName("write should throw an exception if writer factory is null")
  public void writeShouldThrowExceptionIfWriterFactoryIsNull() {
    FlowBuilder<Message, String, String> builder = new FlowBuilder<>();
    Throwable exception = assertThrows(NullPointerException.class, () -> {
      builder.read(message -> null);
      builder.write((Supplier<Function<String, Message>>) null);
    });
    assertThat(exception.getMessage()).contains("writer factory");
  }

  @Test
  @DisplayName("build should create an working Flow if only reader and writer are set")
  public void buildWithOnlyReadAndWrite() {
    Flow flow = new FlowBuilder<DefaultMessage, String, String>()
        .read(DefaultMessage::getId)
        .write(DefaultMessage::withId)
        .build();
    String message = "Whiskey in the Jar";
    Message result = flow.process(DefaultMessage.withId(message));
    assertThat(result.getId()).isEqualTo(message);
  }

}
