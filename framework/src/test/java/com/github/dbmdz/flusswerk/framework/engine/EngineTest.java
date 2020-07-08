package com.github.dbmdz.flusswerk.framework.engine;

import static com.github.dbmdz.flusswerk.framework.fixtures.Flows.consumingFlow;
import static com.github.dbmdz.flusswerk.framework.fixtures.Flows.flowThrowing;
import static com.github.dbmdz.flusswerk.framework.fixtures.Flows.passthroughFlow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dbmdz.flusswerk.framework.exceptions.RetryProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.fixtures.Flows;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.MessageBroker;
import com.github.dbmdz.flusswerk.framework.reporting.SilentProcessReport;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The Engine")
class EngineTest {

  private MessageBroker messageBroker;

  private Message message;

  @BeforeEach
  void setUp() {
    messageBroker = mock(MessageBroker.class);
    message = new Message();
  }

  private Engine createEngine(Flow flow) {
    return new Engine(messageBroker, flow, 5, new SilentProcessReport());
  }

  @Test
  @DisplayName("should use the maximum number of workers")
  public void engineShouldUseMaxNumberOfWorkers() throws IOException, InterruptedException {
    when(messageBroker.receive()).thenReturn(new Message("White Room"));
    Engine engine = createEngine(Flows.flowBlockingAllThreads());
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(engine::start);

    long start = System.currentTimeMillis();
    int timeout = 5 * 60 * 300;
    while (!engine.getStats().allWorkersBusy() && System.currentTimeMillis() - start < timeout) {
      TimeUnit.MILLISECONDS.sleep(100);
    }

    var engineStats = engine.getStats();
    assertThat(engineStats.getActiveWorkers())
        .as(
            "There were %d workers expected, but only %d running after waiting for %d ms",
            engineStats.getConcurrentWorkers(),
            engineStats.getActiveWorkers(),
            System.currentTimeMillis() - start)
        .isEqualTo(engineStats.getConcurrentWorkers());

    engine.stop();
  }

  @Test
  @DisplayName("should reject a message when processing fails")
  void processShouldRejectMessageOnFailure() throws IOException {
    Engine engine = createEngine(flowThrowing(RuntimeException.class));
    engine.process(message);

    verify(messageBroker).reject(message);
    verify(messageBroker, never()).ack(message);
  }

  @Test
  @DisplayName("should accept a message processed without failure")
  void processShouldAcceptMessageWithoutFailure() throws IOException {
    Engine engine = createEngine(passthroughFlow());
    engine.process(message);

    verify(messageBroker).ack(message);
    verify(messageBroker, never()).reject(message);
  }

  @Test
  @DisplayName("should not try to send messages if the writer creates none")
  void shouldNotTryToSendMessagesIfTheWriterCreatesNone() throws IOException {
    Engine engine = createEngine(consumingFlow());
    engine.process(new Message());
    verify(messageBroker, never()).send(any());
  }

  @Test
  @DisplayName("should send a message")
  void processShouldSendMessage() throws IOException {
    Engine engine = createEngine(passthroughFlow());
    engine.process(new Message());
    verify(messageBroker).send(anyCollection());
  }

  @Test
  @DisplayName("should stop with retry for RetryProcessException")
  void retryProcessExceptionShouldRejectTemporarily() throws IOException {
    Engine engine = createEngine(flowThrowing(RetryProcessingException.class));
    engine.process(message);

    verify(messageBroker).reject(message);
  }

  @Test
  @DisplayName("should stop processing for good for StopProcessingException")
  void shouldFailMessageForStopProcessingException() throws IOException {
    Engine engine = createEngine(flowThrowing(StopProcessingException.class));
    engine.process(message);

    verify(messageBroker).fail(message);
  }

  @Test
  @DisplayName("should stop processing for good when sending messages fails")
  void shouldStopProcessingWhenSendingFails() throws IOException {
    doThrow(RuntimeException.class).when(messageBroker).send(anyCollection());
    Engine engine = createEngine(passthroughFlow());
    engine.process(message);
    verify(messageBroker).fail(message);
  }
}
