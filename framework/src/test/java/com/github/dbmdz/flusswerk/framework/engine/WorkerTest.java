package com.github.dbmdz.flusswerk.framework.engine;

import static org.junit.jupiter.params.provider.Arguments.arguments;
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
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("The Worker")
class WorkerTest {

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
    flow = mock(Flow.class);
    messageBroker = mock(MessageBroker.class);
    processReport = mock(ProcessReport.class);
    taskQueue = new PriorityBlockingQueue<>();
    tracing = mock(Tracing.class);
    worker = new Worker(flow, messageBroker, processReport, taskQueue, tracing);
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
    worker.process(message);
    verify(tracing).register(message.getTracing());
  }

  @DisplayName("will fail message on StopProcessingException")
  void willFailMessageOnStopProcessingException() throws IOException {
    when(flow.process(message)).thenThrow(new StopProcessingException("Intentional Exception"));
    worker.process(message);
    verify(messageBroker).fail(message);
  }

  @DisplayName("will retry message on RetryProcessingException")
  @ParameterizedTest
  @MethodSource("retryableExceptions")
  void willRetryMessageOnRetryProcessingException(RuntimeException exception) throws IOException {
    when(flow.process(message)).thenThrow(exception);
    worker.process(message);
    verify(messageBroker).reject(message);
  }

  @DisplayName("will acknowledge message")
  @Test
  void willAcknowledgeMessage() throws IOException {
    worker.process(message);
    verify(messageBroker).ack(message);
  }
}
