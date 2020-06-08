package com.github.dbmdz.flusswerk.framework.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dbmdz.flusswerk.framework.exceptions.RetryProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.ReportFunction;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
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

  private Flow<Message, Message, Message> passthroughFlow() {
    return FlowBuilder.messageProcessor(Message.class).process(m -> m).build();
  }

  private Flow<Message, String, String> flowWithTransformer(Function<String, String> transformer) {
    return FlowBuilder.flow(Message.class, String.class, String.class)
        .reader(Message::getTracingId)
        .transformer(transformer)
        .writerSendingMessage(Message::new)
        .build();
  }

  private Flow<Message, String, String> flowThrowing(Class<? extends RuntimeException> cls) {
    var message = String.format("Generated %s for unit test", cls.getSimpleName());
    final RuntimeException exception;
    try {
      exception = cls.getConstructor(String.class).newInstance(message);
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException e) {
      throw new Error("Could not instantiate exception", e); // If test is broken give up
    }
    Function<String, String> transformerWithException =
        s -> {
          throw exception;
        };
    return flowWithTransformer(transformerWithException);
  }

  @Test
  @DisplayName("should use the maximum number of workers")
  public void engineShouldUseMaxNumberOfWorkers() throws IOException, InterruptedException {
    when(messageBroker.receive()).thenReturn(new Message("White Room"));

    Engine engine =
        new Engine(
            "app",
            messageBroker,
            flowWithTransformer(
                new ThreadBlockingTransformer<>()) // Force engine to use all worker threads
            );
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
    Function<String, String> transformerWithException =
        s -> {
          throw new RuntimeException("Aaaaaaah!");
        };

    var flow = flowWithTransformer(transformerWithException);

    Engine engine = new Engine("app", messageBroker, flow);

    engine.process(message);

    verify(messageBroker).reject(message);
    verify(messageBroker, never()).ack(message);
  }

  @Test
  @DisplayName("should accept a message processed without failure")
  void processShouldAcceptMessageWithoutFailure() throws IOException {
    Engine engine = new Engine("app", messageBroker, passthroughFlow());
    engine.process(message);

    verify(messageBroker).ack(message);
    verify(messageBroker, never()).reject(message);
  }

  @Test
  @DisplayName("should send a message")
  void processShouldSendMessage() throws IOException {
    Engine engine = new Engine("app", messageBroker, passthroughFlow());
    engine.process(new Message());
    verify(messageBroker).send(anyCollection());
  }

  @Test
  @DisplayName("should stop with retry for RetriableProcessException")
  void retryProcessExceptionShouldRejectTemporarily() throws IOException {
    Engine engine = new Engine("app", messageBroker, flowThrowing(RetryProcessingException.class));
    engine.process(message);

    verify(messageBroker).reject(message);
  }

  @Test
  @DisplayName("should stop processing for good for StopProcessingException")
  void shouldFailMessageForStopProcessingException() throws IOException {
    Engine engine = new Engine("app", messageBroker, flowThrowing(StopProcessingException.class));
    engine.process(message);

    verify(messageBroker).fail(message);
  }

  @Test
  @DisplayName("should use functional reporter")
  void testFunctionalReporter() {
    final AtomicBoolean reportHasBeenCalled = new AtomicBoolean(false);
    ReportFunction reportFn = (r, msg, e) -> reportHasBeenCalled.set(true);
    Engine engine =
        new Engine("app", messageBroker, flowThrowing(StopProcessingException.class), reportFn);
    engine.process(new Message());
    assertThat(reportHasBeenCalled.get()).isTrue();
  }

  @Test
  @DisplayName("should stop processing for good when sending messages fails")
  void shouldStopProcessingWhenSendingFails() throws IOException {
    doThrow(RuntimeException.class).when(messageBroker).send(anyCollection());
    Engine engine = new Engine("app", messageBroker, passthroughFlow());
    engine.process(message);
    verify(messageBroker).fail(message);
  }
}
