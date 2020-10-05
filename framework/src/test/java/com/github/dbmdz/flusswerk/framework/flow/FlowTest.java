package com.github.dbmdz.flusswerk.framework.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.dbmdz.flusswerk.framework.TestMessage;
import com.github.dbmdz.flusswerk.framework.fixtures.Flows;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.locking.NoOpLockManager;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.monitoring.FlowMetrics;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

@DisplayName("The Flow")
class FlowTest {

  @AfterEach
  void afterEach() {
    MDC.clear();
  }

  @DisplayName("should not replace tracing ids")
  @Test
  void shouldNotReplaceTracingIds() {
    var expected = new Message("999");
    Flow flow = Flows.messageProcessor(message -> expected);
    var actual = flow.process(new Message("123"));
    assertThat(actual).containsExactly(expected);
  }

  @DisplayName("should never return null for message processor")
  @Test
  void shouldNeverReturnNullForProcessor() {
    Flow flow = Flows.messageProcessor(message -> null);
    var actual = flow.process(new Message("123"));
    assertThat(actual).isEmpty();
  }

  @DisplayName("should never return null for writer")
  @Test
  void shouldNeverReturnNullForWriter() {
    FlowSpec flowSpec =
        FlowBuilder.flow(Message.class, Message.class, Message.class)
            .reader(m -> m)
            .transformer(m -> m)
            .writerSendingMessage(m -> null)
            .build();
    Flow flow = new Flow(flowSpec, new NoOpLockManager(), new Tracing());
    var actual = flow.process(new Message("123"));
    assertThat(actual).isEmpty();
  }

  @DisplayName("should collect metrics with collector from builder")
  @Test
  void shouldCollectMetricsWithCollectorFromBuilder() {
    FlowMetrics metrics = mock(FlowMetrics.class);
    FlowSpec flowSpec =
        FlowBuilder.messageProcessor(Message.class).process(m -> m).metrics(metrics).build();
    Flow flow = new Flow(flowSpec, new NoOpLockManager(), new Tracing());
    flow.process(new Message("123"));
    verify(metrics).accept(any());
  }

  @DisplayName("should collect metrics registered by Spring")
  @Test
  void shouldCollectMetricsRegisteredBySpring() {
    FlowMetrics metrics = mock(FlowMetrics.class);
    Flow flow = Flows.messageProcessor(m -> m);
    flow.registerFlowMetrics(Set.of(metrics));
    flow.process(new Message("123"));
    verify(metrics).accept(any());
  }

  @DisplayName("should set id for logging if present")
  @Test
  void shouldSetIdForLoggingIfPresent() {
    Flow flow = Flows.messageProcessor(m -> m);
    assertThat(MDC.get("id")).isNull();
    flow.setLoggingData(new TestMessage("123"));
    assertThat(MDC.get("id")).isEqualTo("123");
  }

  @DisplayName("should set tracing id for logging if present")
  @Test
  void shouldSetTracingIdForLoggingIfPresent() {
    Flow flow = Flows.messageProcessor(m -> m);
    assertThat(MDC.get("tracingId")).isNull();
    flow.setLoggingData(new Message("123"));
    assertThat(MDC.get("tracingId")).isEqualTo("123");
  }

  @DisplayName("should generate tracing id if none is present")
  void shouldGenerateTracingIdIfNoneIsPresent() {
    Flow flow = Flows.messageProcessor(m -> m);
    Collection<Message> actual = flow.process(new Message());
    assertThat(actual).allSatisfy(message -> assertThat(message.getTracingId()).isNotBlank());
  }

  @DisplayName("should set generated tracing id if none is present")
  void shouldSetTracingIdIfNoneIsPresent() {
    Flow flow = Flows.messageProcessor(m -> m);
    assertThat(MDC.get("tracingId")).isNull();
    flow.process(new Message());
    assertThat(MDC.get("tracingId")).isNotBlank();
  }

  @DisplayName("should generate different tracing ids")
  void shouldGenerateDifferentTracingIds() {
    Flow flow = Flows.messageProcessor(m -> m);
    Collection<String> firstTracingIds =
        flow.process(new Message()).stream()
            .map(Message::getTracingId)
            .collect(Collectors.toList());
    Collection<String> secondTracingIds =
        flow.process(new Message()).stream()
            .map(Message::getTracingId)
            .collect(Collectors.toList());
    assertThat(firstTracingIds).isNotEqualTo(secondTracingIds);
  }
}
