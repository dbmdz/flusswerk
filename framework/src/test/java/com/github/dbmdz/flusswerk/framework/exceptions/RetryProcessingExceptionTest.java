package com.github.dbmdz.flusswerk.framework.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("The RetryProcessingException")
class RetryProcessingExceptionTest {

  record Fixture(String description, List<Message> messagesToRetry, List<Message> messagesToSend) {}

  static Stream<Fixture> messages() {
    var retry = List.of(new Message());
    var send = List.of(new Message());
    return Stream.of(
        new Fixture("regular", List.of(), List.of()),
        new Fixture("only retry", retry, List.of()),
        new Fixture("only send", List.of(), send),
        new Fixture("send and retry", retry, send));
  }

  @ParameterizedTest
  @MethodSource("messages")
  void testMessages(Fixture fixture) {
    var rpe =
        new RetryProcessingException(fixture.description)
            .retry(fixture.messagesToRetry)
            .send(fixture.messagesToSend);
    assertThat(rpe.getMessagesToRetry()).isEqualTo(fixture.messagesToRetry);
    assertThat(rpe.getMessagesToSend()).isEqualTo(fixture.messagesToSend);
    assertThat(rpe.hasMessagesToRetry()).isEqualTo(!fixture.messagesToRetry.isEmpty());
    assertThat(rpe.hasMessagesToSend()).isEqualTo(!fixture.messagesToSend.isEmpty());
  }
}
