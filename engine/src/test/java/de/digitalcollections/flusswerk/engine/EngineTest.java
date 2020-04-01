package de.digitalcollections.flusswerk.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.digitalcollections.flusswerk.engine.exceptions.FinallyFailedProcessException;
import de.digitalcollections.flusswerk.engine.exceptions.RetriableProcessException;
import de.digitalcollections.flusswerk.engine.exceptions.RetryProcessingException;
import de.digitalcollections.flusswerk.engine.exceptions.StopProcessingException;
import de.digitalcollections.flusswerk.engine.flow.Flow;
import de.digitalcollections.flusswerk.engine.flow.FlowBuilder;
import de.digitalcollections.flusswerk.engine.messagebroker.MessageBroker;
import de.digitalcollections.flusswerk.engine.model.DefaultMessage;
import de.digitalcollections.flusswerk.engine.model.Message;
import de.digitalcollections.flusswerk.engine.reporting.ReportFunction;
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

  private Flow<DefaultMessage, String, String> flowSendingMessage() {
    return flowWithTransformer(Function.identity());
  }

  private Flow<DefaultMessage, String, String> flowWithTransformer(
      Function<String, String> transformer) {
    return new FlowBuilder<DefaultMessage, String, String>()
        .read(DefaultMessage::getId)
        .transform(transformer)
        .writeAndSend((Function<String, Message>) DefaultMessage::new)
        .build();
  }

  private Flow<DefaultMessage, String, String> flowThrowing(Class<? extends RuntimeException> cls) {
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
    when(messageBroker.receive()).thenReturn(new DefaultMessage("White Room"));

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
    Message<String> message = new DefaultMessage();

    engine.process(message);

    verify(messageBroker).reject(message);
    verify(messageBroker, never()).ack(message);
  }

  @Test
  @DisplayName("should accept a message processed without failure")
  void processShouldAcceptMessageWithoutFailure() throws IOException {
    Engine engine = new Engine(messageBroker, flowSendingMessage());
    Message<String> message = new DefaultMessage();
    engine.process(message);

    verify(messageBroker).ack(message);
    verify(messageBroker, never()).reject(message);
  }

  @Test
  @DisplayName("should send a message")
  void processShouldSendMessage() throws IOException {
    Engine engine = new Engine(messageBroker, flowSendingMessage());
    engine.process(new DefaultMessage());
    verify(messageBroker).send(any(Message.class));
  }

  @Test
  @DisplayName("should stop with retry for RetriableProcessException")
  void retriableProcessExceptionShallRejectTemporarily() throws IOException {
    Engine engine = new Engine(messageBroker, flowThrowing(RetriableProcessException.class));
    Message<?> message = new DefaultMessage();
    engine.process(message);

    verify(messageBroker).reject(message);
  }

  @Test
  @DisplayName("should stop with retry for RetriableProcessException")
  void retryProcessExceptionShouldRejectTemporarily() throws IOException {
    Engine engine = new Engine(messageBroker, flowThrowing(RetryProcessingException.class));
    Message<?> message = new DefaultMessage();
    engine.process(message);

    verify(messageBroker).reject(message);
  }

  @Test
  @DisplayName("should stop processing for good for FinallyFailedProcessException")
  void finallyFailedProcessExceptionShallFailMessage() throws IOException {
    Engine engine = new Engine(messageBroker, flowThrowing(FinallyFailedProcessException.class));
    Message<?> message = new DefaultMessage();
    engine.process(message);

    verify(messageBroker).fail(message);
  }

  @Test
  @DisplayName("should stop processing for good for StopProcessingException")
  void shoudlFailMessageForStopProcessingException() throws IOException {
    Engine engine = new Engine(messageBroker, flowThrowing(StopProcessingException.class));
    Message<?> message = new DefaultMessage();
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
    engine.process(new DefaultMessage());
    assertThat(reportHasBeenCalled).isTrue();
  }
}
