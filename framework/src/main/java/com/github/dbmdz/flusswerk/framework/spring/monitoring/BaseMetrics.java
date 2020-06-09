package com.github.dbmdz.flusswerk.framework.spring.monitoring;

import com.github.dbmdz.flusswerk.framework.flow.FlowMetrics;
import com.github.dbmdz.flusswerk.framework.flow.FlowMetrics.Status;
import io.micrometer.core.instrument.Counter;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/** Collect metrics on flow execution. */
public class BaseMetrics implements Consumer<FlowMetrics> {

  private final Map<Status, Counter> executionTime;
  private final Map<Status, Counter> processedItems;

  public BaseMetrics(MeterFactory meterFactory) {
    this.executionTime = new EnumMap<>(Status.class);
    this.processedItems = new EnumMap<>(Status.class);

    for (Status status : Status.values()) {
      processedItems.put(status, meterFactory.counter("processed.items", status));
      executionTime.put(status, meterFactory.counter("execution.time", status));
    }
  }

  public void accept(FlowMetrics flowMetrics) {
    var status = flowMetrics.getStatus();
    processedItems.get(status).increment();
    executionTime.get(status).increment(flowMetrics.duration());
  }
}
