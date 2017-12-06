package de.digitalcollections.workflow.engine.model;

import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;

class JobTest {

  private static final Message SOME_MESSAGE = DefaultMessage.withType("Hey!");

  private static final Function<Message, String> DUMMY_READER = Message::getType;

  private static final Function<String, String> DUMMY_TRANSFORMER = Function.identity();

  private static final Function<String, Message> DUMMY_WRITER = DefaultMessage::new;


  class CheckIfCalled<R, W> implements Function<R, W> {

    boolean called = false;

    @Override
    public W apply(R r) {
      called = true;
      return null;
    }
  }

  @BeforeEach
  void setUp() {
  }

  @Test
  @DisplayName("Read should call the read function")
  void read() {
    CheckIfCalled<Message, String> reader = new CheckIfCalled<>();
    Job<Message, String, String> job = new Job<>(SOME_MESSAGE);
    job.read(reader);
    assertThat(reader.called).isTrue();
  }

  @Test
  void transform() {
    CheckIfCalled<String, String> transformer = new CheckIfCalled<>();
    Job<Message, String, String> job = new Job<>(SOME_MESSAGE);
    job.transform(transformer);
    assertThat(transformer.called).isTrue();
  }

  @Test
  void write() {
    CheckIfCalled<String, Message> writer = new CheckIfCalled<>();
    Job<Message, String, String> job = new Job<>(SOME_MESSAGE);
    job.write(writer);
    assertThat(writer.called).isTrue();
  }

  @Test
  @DisplayName("Read, Transform, Write should pass values along")
  void readTransformWriteShouldPassValues() {
    String message = "Jolene, Jolene, Jolene, Jolene";
    Job<Message, String, String> job = new Job<>(DefaultMessage.withType(message));
    job.read(Message::getType);
    job.transform(String::toUpperCase);
    job.write(DefaultMessage::new);
    assertThat(job.getResult()).returns(message.toUpperCase(), from(Message::getType));
  }

  @Test
  void getMessage() {
    Message message = DefaultMessage.withType("Wuthering Heights");
    Job<Message, String, String> job = new Job<>(message);
    assertThat(job.getMessage()).isEqualTo(message);
  }

}
