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

  private final Map<Status, Counter> executionTime;
  private final Map<Status, Counter> processedItems;

  public DefaultFlowMetrics(MeterFactory meterFactory) {
    this.executionTime = new EnumMap<>(Status.class);
    this.processedItems = new EnumMap<>(Status.class);

    for (Status status : Status.values()) {
      processedItems.put(status, meterFactory.counter("processed.items", status));
      executionTime.put(status, meterFactory.counter("execution.time", status));
    }
  }

  public void accept(FlowInfo flowInfo) {
    var status = flowInfo.getStatus();
    processedItems.get(status).increment();
    executionTime.get(status).increment(flowInfo.duration());
  }
}
