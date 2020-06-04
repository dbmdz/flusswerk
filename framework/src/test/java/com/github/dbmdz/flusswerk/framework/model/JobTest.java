package com.github.dbmdz.flusswerk.framework.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.flow.builder.TestMessage;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The Job")
class JobTest {

  private static final Function<Message, String> READ_NOTHING = m -> "";

  private static final Function<String, String> DO_NOTHING = Function.identity();

  private static final Function<String, Collection<Message>> CREATE_EMPTY_MESSAGE = s -> List.of(new Message());

  static class CheckIfCalled<R, W> implements Function<R, W> {

    boolean called = false;

    @Override
    public W apply(R r) {
      called = true;
      return null;
    }
  }

  static class CheckIfWritten<R> implements Function<R, Collection<Message>> {

    boolean called = false;

    @Override
    public Collection<Message> apply(R r) {
      called = true;
      return null;
    }
  }

  private Supplier<RuntimeException> exception(String message) {
    return () -> new RuntimeException(message);
  }

  @Test
  @DisplayName("should call the reader")
  void read() {
    CheckIfCalled<Message, String> reader = new CheckIfCalled<>();
    Job<Message, String, String> job = new Job<>(new Message());
    job.read(reader);
    assertThat(reader.called).isTrue();
  }

  @Test
  @DisplayName("should call the transformer")
  void transform() {
    CheckIfCalled<String, String> transformer = new CheckIfCalled<>();
    Job<Message, String, String> job = new Job<>(new Message());
    job.transform(transformer);
    assertThat(transformer.called).isTrue();
  }

  @Test
  @DisplayName("should call the writer")
  void write() {
    CheckIfWritten<String> writer = new CheckIfWritten<>();
    Job<Message, String, String> job = new Job<>(new Message());
    job.write(writer);
    assertThat(writer.called).isTrue();
  }

  @Test
  @DisplayName("should pass values through Read, Transform and Write")
  void readTransformWriteShouldPassValues() {
    TestMessage message = new TestMessage("testid");
    Job<TestMessage, String, String> job = new Job<>(message);
    job.read(TestMessage::getId);
    job.transform(String::toUpperCase);
    job.write(
        (Function<String, Collection<Message>>) id -> Collections.singleton(new TestMessage(id)));

    var actual = (TestMessage) unbox(job.getResult());
    assertThat(actual.getId()).isEqualTo(message.getId().toUpperCase());
  }

  @Test
  @DisplayName("should propagate flow ids")
  void shouldPropagateTracingIds() {
    Message incomingMessage = new Message("12345");
    Job<Message, String, String> job = new Job<>(incomingMessage);
    job.read(READ_NOTHING);
    job.transform(DO_NOTHING);
    job.write(CREATE_EMPTY_MESSAGE);

    var actual = unbox(job.getResult());
    assertThat(actual.getTracingId()).isEqualTo(incomingMessage.getTracingId());
  }


  @Test
  @DisplayName("should not overwrite manually set flow ids")
  void shouldNotOverwriteManualTracingIds() {
    Message incomingMessage = new Message("12345");
    Job<Message, String, String> job = new Job<>(incomingMessage);
    job.read(READ_NOTHING);
    job.transform(DO_NOTHING);

    String expectedTracingId = "abc";
    job.write(s -> List.of(new Message(expectedTracingId)));

    var actual = unbox(job.getResult());
    assertThat(actual.getTracingId()).isEqualTo(expectedTracingId);
  }

  private Message unbox(Collection<Message> collection) {
    return collection.stream().findFirst().orElseThrow();
  }
}
