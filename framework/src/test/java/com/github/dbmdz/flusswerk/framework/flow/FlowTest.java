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
import java.util.Set;
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
    Flow flow = new Flow(flowSpec, new NoOpLockManager());
    var actual = flow.process(new Message("123"));
    assertThat(actual).isEmpty();
  }

  @DisplayName("should collect metrics with collector from builder")
  @Test
  void shouldCollectMetricsWithCollectorFromBuilder() {
    FlowMetrics metrics = mock(FlowMetrics.class);
    FlowSpec flowSpec =
        FlowBuilder.messageProcessor(Message.class).process(m -> m).metrics(metrics).build();
    Flow flow = new Flow(flowSpec, new NoOpLockManager());
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
}
