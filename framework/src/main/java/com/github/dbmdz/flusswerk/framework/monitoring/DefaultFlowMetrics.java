package com.github.dbmdz.flusswerk.framework.monitoring;

import com.github.dbmdz.flusswerk.framework.flow.FlowInfo;
import io.micrometer.core.instrument.Counter;
import java.util.EnumMap;
import java.util.Map;

/**
 * Collect metrics on flow execution. This implementation is default if there are no other
 * FlowMetrics beans defined. You can define as many FlowMetrics beans as needed.
 */
public class DefaultFlowMetrics implements FlowMetrics {

  protected final Map<Status, Counter> messagesTotal;
  protected final Map<Status, Counter> messagesSeconds;

  @Deprecated(since = "5.1.0", forRemoval = true)
  private final Map<Status, Counter> executionTime;

  @Deprecated(since = "5.1.0", forRemoval = true)
  private final Map<Status, Counter> processedItems;

  public DefaultFlowMetrics(MeterFactory meterFactory) {
    messagesTotal = new EnumMap<>(Status.class);
    messagesSeconds = new EnumMap<>(Status.class);
    this.executionTime = new EnumMap<>(Status.class);
    this.processedItems = new EnumMap<>(Status.class);

    for (Status status : Status.values()) {
      processedItems.put(status, meterFactory.counter("processed.items", status));
      executionTime.put(status, meterFactory.counter("execution.time", status));
      messagesTotal.put(status, meterFactory.frameworkCounter("messages.total", status));
      messagesSeconds.put(status, meterFactory.frameworkCounter("messages.seconds", status));
    }
  }

  public void accept(FlowInfo flowInfo) {
    var status = flowInfo.getStatus();
    processedItems.get(status).increment();
    executionTime.get(status).increment(flowInfo.duration());
    messagesTotal.get(status).increment();
    messagesSeconds.get(status).increment(flowInfo.duration());
  }
}
