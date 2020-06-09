package com.github.dbmdz.flusswerk.spring.boot.example;

import com.github.dbmdz.flusswerk.framework.flow.FlowMetrics;
import com.github.dbmdz.flusswerk.framework.flow.FlowMetrics.Status;
import com.github.dbmdz.flusswerk.framework.spring.monitoring.MeterFactory;
import io.micrometer.core.instrument.Counter;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class Metrics implements Consumer<FlowMetrics> {

  private final Map<Status, Counter> executionTime;

  private final Map<Status, Counter> processedItems;

  public Metrics(MeterFactory meterFactory) {
    this.executionTime = new EnumMap<>(Status.class);
    this.processedItems = new EnumMap<>(Status.class);

    for (Status status : Status.values()) {
      processedItems.put(status, meterFactory.counter("processed.items", status));
      executionTime.put(status, meterFactory.counter("execution.time", status));
    }
  }

  /**
   * Counter for the total execution time per {@link Status}.
   *
   * @param status the respective status
   * @return the corresponding counter
   */
  public Counter executionTime(Status status) {
    return executionTime.get(status);
  }

  /**
   * Counter for the total number of processed items per {@link Status}.
   *
   * @param status the respective status
   * @return the corresponding counter
   */
  public Counter processedItems(Status status) {
    return processedItems.get(status);
  }

  public void accept(FlowMetrics flowMetrics) {
    var status = flowMetrics.getStatus();
    processedItems.get(status).increment();
    executionTime.get(status).increment(flowMetrics.duration());
  }
}
