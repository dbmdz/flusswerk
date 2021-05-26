package com.github.dbmdz.flusswerk.framework.engine;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dbmdz.flusswerk.framework.exceptions.RetryProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.MessageBroker;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("The Worker")
class WorkerTest {

  private Semaphore availableWorkers;
  private Worker worker;
  private Flow flow;
  private Tracing tracing;
  private MessageBroker messageBroker;
  private ProcessReport processReport;
  private PriorityBlockingQueue<Task> taskQueue;
  private Message message;

  private static Stream<Arguments> retryableExceptions() {
    return Stream.of(
        arguments(new RetryProcessingException("Intentional")),
        arguments(new RuntimeException("Intentional")));
  }

  @BeforeEach
  void setUp() {
    availableWorkers = mock(Semaphore.class);
    flow = mock(Flow.class);
    messageBroker = mock(MessageBroker.class);
    processReport = mock(ProcessReport.class);
    taskQueue = new PriorityBlockingQueue<>();
    tracing = mock(Tracing.class);
    worker = new Worker(availableWorkers, flow, messageBroker, processReport, taskQueue, tracing);
    message = new Message("tracing id");
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
  void shouldFailMessageOnStopProcessingException() throws IOException {
    when(flow.process(message)).thenThrow(new StopProcessingException("Intentional Exception"));
    worker.process(message);
    verify(messageBroker).fail(message);
  }

  @DisplayName("should retry message on RetryProcessingException")
  @ParameterizedTest
  @MethodSource("retryableExceptions")
  void shouldRetryMessageOnRetryProcessingException(RuntimeException exception) throws IOException {
    when(flow.process(message)).thenThrow(exception);
    worker.process(message);
    verify(messageBroker).reject(message);
  }

  @DisplayName("should acknowledge message")
  @Test
  void shouldAcknowledgeMessage() throws IOException {
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
  void shouldLogRetry() throws IOException {
    Exception exception = new RetryProcessingException("intentional");
    when(flow.process(message)).thenThrow(exception);
    when(messageBroker.reject(message)).thenReturn(true); // retry
    worker.process(message);
    verify(processReport).reportReject(any(), any(Exception.class));
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
  void shouldSendMessages() throws IOException {
    when(flow.process(message)).thenReturn(List.of(message));
    worker.process(message);
    verify(messageBroker).send(List.of(message));
  }

  @DisplayName("should fail processing when sending messages fails")
  @Test
  void shouldFailProcessingWhenSendingMessagesFails() throws IOException {
    when(flow.process(message)).thenReturn(List.of(message));
    doThrow(IOException.class).when(messageBroker).send(any());
    worker.process(message);
    verify(processReport).reportFail(any(), any());
  }

  @DisplayName("should release semaphore")
  @Test
  void shouldReleaseSemaphore() throws IOException {
    worker.executeProcessing(message);
    verify(availableWorkers).release();
  }
}
