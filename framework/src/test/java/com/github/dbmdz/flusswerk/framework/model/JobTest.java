package com.github.dbmdz.flusswerk.framework.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.flow.builder.TestMessage;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JobTest {

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
  @DisplayName("Read should call the read function")
  void read() {
    CheckIfCalled<Message, String> reader = new CheckIfCalled<>();
    Job<Message, String, String> job = new Job<>(new Message());
    job.read(reader);
    assertThat(reader.called).isTrue();
  }

  @Test
  void transform() {
    CheckIfCalled<String, String> transformer = new CheckIfCalled<>();
    Job<Message, String, String> job = new Job<>(new Message());
    job.transform(transformer);
    assertThat(transformer.called).isTrue();
  }

  @Test
  void write() {
    CheckIfWritten<String> writer = new CheckIfWritten<>();
    Job<Message, String, String> job = new Job<>(new Message());
    job.write(writer);
    assertThat(writer.called).isTrue();
  }

  @Test
  @DisplayName("Read, Transform, Write should pass values along")
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
  @DisplayName("Should propagate flow ids")
  void propagateTracingIds() {
    Message incomingMessage = new Message("12345");
    Job<Message, String, String> job = new Job<>(incomingMessage, true);
    job.read(m -> "");
    job.transform(Function.identity());
    job.write((Function<String, Collection<Message>>) s -> List.of(new Message()));

    var actual = unbox(job.getResult());
    assertThat(actual.getTracingId()).isEqualTo(incomingMessage.getTracingId());
  }

  private Message unbox(Collection<Message> collection) {
    return collection.stream().findFirst().orElseThrow();
  }
}
