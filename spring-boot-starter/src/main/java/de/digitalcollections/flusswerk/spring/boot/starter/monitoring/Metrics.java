package de.digitalcollections.flusswerk.spring.boot.starter.monitoring;

import de.digitalcollections.flusswerk.engine.flow.FlowStatus;
import de.digitalcollections.flusswerk.engine.flow.FlowStatus.Status;
import io.micrometer.core.instrument.Counter;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/** Collect metrics on flow execution. */
public class Metrics implements Consumer<FlowStatus> {

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

  public void accept(FlowStatus flowStatus) {
    var status = flowStatus.getStatus();
    processedItems.get(status).increment();
    executionTime.get(status).increment(flowStatus.duration());
  }
}
