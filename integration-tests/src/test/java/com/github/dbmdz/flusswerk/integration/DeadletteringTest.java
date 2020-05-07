package com.github.dbmdz.flusswerk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Feature: Dead letter messages
 *
 * <p>Scenario: Message processing fails constantly Given I have an message broker with default
 * config and a message in integration.in When the processing always fails Then the message in queue
 * integration.in.failed has 5 retries And integration.in is empty And integration.out is empty
 */
public class DeadletteringTest {

  private static final String QUEUE_IN = "test.in";
  private static final String QUEUE_OUT = "test.out";
  private static final String QUEUE_FAILED = "test.in.failed";
  private static final String QUEUE_RETRY = "test.in.retry";

  private Backend backend;

  @BeforeEach
  public void setUp() {
    backend = new Backend(QUEUE_IN, QUEUE_OUT);
  }

  @Test
  public void failingMessagesShouldEndInFailedQueue() throws Exception {
    MessageBroker messageBroker = backend.getMessageBroker();
    var flow =
        new FlowBuilder<>()
            .read(Message::toString)
            .transform(
                s -> {
                  throw new RuntimeException("Fail!");
                })
            .write(s -> new Message(s.toString()))
            .build();

    Engine engine = new Engine(messageBroker, flow);

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(engine::start);
    messageBroker.send(QUEUE_IN, new Message("123456"));

    Message message = backend.waitForMessageFrom(QUEUE_FAILED, 10_000);
    assertThat(message.getTracingId()).isEqualTo("123456");
    assertThatMessageFrom(QUEUE_IN).isNull();
    assertThatMessageFrom(QUEUE_OUT).isNull();
    assertThatMessageFrom(QUEUE_RETRY).isNull();

    engine.stop();
    executorService.shutdownNow();
  }

  private ObjectAssert<? extends Message> assertThatMessageFrom(String name) throws Exception {
    return assertThat(backend.waitForMessageFrom(QUEUE_FAILED, 1000));
  }
}
