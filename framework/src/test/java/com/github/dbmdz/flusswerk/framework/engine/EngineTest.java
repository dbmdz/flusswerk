package com.github.dbmdz.flusswerk.framework.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

  @BeforeEach
  void setUp() {
    messageBroker = mock(MessageBroker.class);
  }

  private Flow<Message, String, String> flowSendingMessage() {
    return flowWithTransformer(Function.identity());
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

    Engine engine = new Engine(messageBroker, flow);
    Message message = new Message();

    engine.process(message);

    verify(messageBroker).reject(message);
    verify(messageBroker, never()).ack(message);
  }

  @Test
  @DisplayName("should accept a message processed without failure")
  void processShouldAcceptMessageWithoutFailure() throws IOException {
    Engine engine = new Engine(messageBroker, flowSendingMessage());
    Message message = new Message();
    engine.process(message);

    verify(messageBroker).ack(message);
    verify(messageBroker, never()).reject(message);
  }

  @Test
  @DisplayName("should send a message")
  void processShouldSendMessage() throws IOException {
    Engine engine = new Engine(messageBroker, flowSendingMessage());
    engine.process(new Message());
    verify(messageBroker).send(any(Message.class));
  }

  @Test
  @DisplayName("should stop with retry for RetriableProcessException")
  void retryProcessExceptionShouldRejectTemporarily() throws IOException {
    Engine engine = new Engine(messageBroker, flowThrowing(RetryProcessingException.class));
    Message message = new Message();
    engine.process(message);

    verify(messageBroker).reject(message);
  }

  @Test
  @DisplayName("should stop processing for good for StopProcessingException")
  void shoudlFailMessageForStopProcessingException() throws IOException {
    Engine engine = new Engine(messageBroker, flowThrowing(StopProcessingException.class));
    Message message = new Message();
    engine.process(message);

    verify(messageBroker).fail(message);
  }

  @Test
  @DisplayName("should use functional reporter")
  void testFunctionalReporter() throws IOException {
    final AtomicBoolean reportHasBeenCalled = new AtomicBoolean(false);
    ReportFunction reportFn = (r, msg, e) -> reportHasBeenCalled.set(true);
    Engine engine =
        new Engine(messageBroker, flowThrowing(StopProcessingException.class), 4, reportFn);
    engine.process(new Message());
    assertThat(reportHasBeenCalled.get()).isTrue();
  }
}
