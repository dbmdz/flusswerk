package com.github.dbmdz.flusswerk.framework.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

import com.github.dbmdz.flusswerk.framework.TestMessage;
import com.github.dbmdz.flusswerk.framework.exceptions.RetryProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.SkipProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.monitoring.FlusswerkMetrics;
import com.github.dbmdz.flusswerk.framework.rabbitmq.MessageBroker;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("The Worker")
@ExtendWith(MockitoExtension.class)
class WorkerTest {

  @Mock private Flow flow;
  @Mock private Tracing tracing;
  @Mock private MessageBroker messageBroker;
  @Mock private ProcessReport processReport;
  @Mock private FlusswerkMetrics flusswerkMetrics;

  @Captor private ArgumentCaptor<Collection<Message>> messagesCaptor;

  private Message message;
  private PriorityBlockingQueue<Task> taskQueue;
  private Worker worker;

  private static Stream<Arguments> retryableExceptions() {
    return Stream.of(
        arguments(new RetryProcessingException("Intentional")),
        arguments(new RuntimeException("Intentional")));
  }

  @BeforeEach
  void setUp() {
    taskQueue = new PriorityBlockingQueue<>();
    worker = new Worker(flow, flusswerkMetrics, messageBroker, processReport, taskQueue, tracing);
    message = new Message();
  }

  @DisplayName("uses underlying Flow")
  @Test
  void usesUnderlyingFlow() {
    worker.process(message);
    verify(flow).process(message);
  }

  @DisplayName("registers tracing information")
  @Test
  void registersTracingInformation() {
    worker.executeProcessing(message);
    verify(tracing).register(message.getTracing());
  }

  @DisplayName("should fail message on StopProcessingException")
  @Test
  void shouldFailMessageOnStopProcessingException() {
    when(flow.process(message)).thenThrow(new StopProcessingException("Intentional Exception"));
    worker.process(message);
    verify(messageBroker).fail(message);
  }

  @DisplayName("should retry message on RetryProcessingException")
  @ParameterizedTest
  @MethodSource("retryableExceptions")
  void shouldRetryMessageOnRetryProcessingException(RuntimeException exception) {
    when(flow.process(message)).thenThrow(exception);
    worker.process(message);
    verify(messageBroker).reject(message);
  }

  @DisplayName("should acknowledge message")
  @Test
  void shouldAcknowledgeMessage() {
    worker.process(message);
    verify(messageBroker).ack(message);
  }

  @DisplayName("should read message from task queue")
  @Test
  void shouldReadMessageFromTaskQueue() {
    Task task = new Task(message, 42);
    taskQueue.put(task);
    worker.step();
    verify(flow).process(message);
  }

  @DisplayName("should log failure on StopProcessingException")
  @Test
  void shouldLogFailure() {
    StopProcessingException exception = new StopProcessingException("intentional");
    when(flow.process(message)).thenThrow(exception);
    worker.process(message);
    verify(processReport).reportFail(any(), eq(exception));
  }

  @DisplayName("should log retry on exception")
  @Test
  void shouldLogRetry() {
    Exception exception = new RetryProcessingException("intentional");
    when(flow.process(message)).thenThrow(exception);
    when(messageBroker.reject(message)).thenReturn(true); // retry
    worker.process(message);
    verify(processReport).reportRetry(any(), any(RuntimeException.class));
  }

  @DisplayName("should log failure after too many retries")
  @Test
  void shouldLogFailureAfterTooManyRetries() {
    message.getEnvelope().setRetries(5);
    Exception exception = new RetryProcessingException("intentional");
    when(flow.process(message)).thenThrow(exception);
    worker.process(message);
    verify(processReport).reportFailAfterMaxRetries(any(), eq(exception));
  }

  @DisplayName("should send messages")
  @Test
  void shouldSendMessages() {
    when(flow.process(message)).thenReturn(List.of(message));
    worker.process(message);
    verify(messageBroker).send(List.of(message));
  }

  @DisplayName("should fail processing when sending messages fails")
  @Test
  void shouldFailProcessingWhenSendingMessagesFails() {
    when(flow.process(message)).thenReturn(List.of(message));
    doThrow(RuntimeException.class).when(messageBroker).send(any());
    worker.process(message);
    verify(processReport).reportFail(any(), any());
  }

  @DisplayName("should confirm that task is done")
  @Test
  void shouldReleaseSemaphore() {
    Runnable callback = mock(Runnable.class);
    Task task = new Task(message, 42, callback);
    taskQueue.put(task);
    worker.step();
    worker.executeProcessing(message);
    verify(callback).run();
  }

  @DisplayName("should register active workers")
  @Test
  void shouldRegisterActiveWorkers() {
    worker.executeProcessing(new Message());
    InOrder inOrder = Mockito.inOrder(flusswerkMetrics, messageBroker);
    inOrder.verify(flusswerkMetrics).incrementActiveWorkers();
    inOrder.verify(messageBroker).ack(any());
    inOrder.verify(flusswerkMetrics).decrementActiveWorkers();
  }

  @DisplayName("should add tracing information to messages after skipping")
  @Test
  void shouldAddTracingAfterSkipping() {
    List<String> tracingPath = List.of("abcde", "1234567");
    Message incomingMessage = new Message();
    incomingMessage.setTracing(tracingPath);

    // setup mocks
    doAnswer(
            invocation -> {
              Collection<? extends Message> messages = invocation.getArgument(0);
              messages.forEach(message -> message.setTracing(tracingPath));
              return null;
            })
        .when(tracing)
        .ensureFor(anyList());

    when(flow.process(incomingMessage))
        .thenThrow(new SkipProcessingException("Skip processing").send(new Message()));

    // run test
    worker.process(incomingMessage);

    // verify
    Message expectedMessage = new Message();
    expectedMessage.setTracing(tracingPath);

    verify(messageBroker).send(messagesCaptor.capture());
    assertThat(unwrapOne(messagesCaptor.getValue()).getTracing()).isEqualTo(tracingPath);
  }

  private Message unwrapOne(Collection<? extends Message> actual) {
    assertThat(actual).hasSize(1);
    return actual.iterator().next();
  }

  @DisplayName("should perform complex retry sending messages")
  @Test
  void shouldPerformComplexRetrySendingMessages() {
    Message incomingMessage = new TestMessage("incoming");
    Message outgoingMessage = new TestMessage("outgoing");
    when(flow.process(incomingMessage))
        .thenThrow(new RetryProcessingException("Retry processing").send(outgoingMessage));
    worker.process(incomingMessage);
    verify(messageBroker).send(List.of(outgoingMessage));
  }

  @DisplayName("should perform complex retry with new messages")
  @Test
  void shouldPerformComplexRetryWithNewMessages() {
    Message incomingMessage = new TestMessage("incoming");
    List<Message> messagesToRetry = List.of(new TestMessage("retry1"), new TestMessage("retry2"));
    when(flow.process(incomingMessage))
        .thenThrow(new RetryProcessingException("Retry processing").retry(messagesToRetry));
    worker.process(incomingMessage);
    verify(messageBroker).ack(incomingMessage);
    for (Message message : messagesToRetry) {
      verify(messageBroker).reject(message);
    }
  }
}
