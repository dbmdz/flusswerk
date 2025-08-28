package dev.mdz.flusswerk.monitoring;

import dev.mdz.flusswerk.config.properties.ProcessingProperties;
import dev.mdz.flusswerk.flow.FlowInfo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FlusswerkMetrics implements FlowMetrics {

  private final AtomicInteger activeWorkers = new AtomicInteger(0);

  private final int totalWorkers;

  protected final Map<Status, Counter> messagesTotal;
  protected final Map<Status, Counter> messagesSeconds;

  public FlusswerkMetrics(ProcessingProperties properties, MeterRegistry registry) {
    this.totalWorkers = properties.threads();

    Gauge.builder("flusswerk.workers", activeWorkers, AtomicInteger::get)
        .description("Number of worker threads in the system")
        .tags(Tags.of("state", "active"))
        .register(registry);

    Gauge.builder("flusswerk.workers", activeWorkers, w -> totalWorkers - w.get())
        .description("Number of worker threads in the system")
        .tags(Tags.of("state", "idle"))
        .register(registry);

    messagesTotal = new EnumMap<>(Status.class);
    messagesSeconds = new EnumMap<>(Status.class);

    for (Status status : Status.values()) {
      String statusTag = status.name().toLowerCase(Locale.ROOT);
      messagesTotal.put(
          status,
          Counter.builder("flusswerk.messages")
              .tag("status", statusTag)
              .description("Total number of messages processed since application start")
              .register(registry));
      messagesSeconds.put(
          status,
          Counter.builder("flusswerk.messages.seconds")
              .tag("status", statusTag)
              .description("Total time spent processing messages since application start")
              .register(registry));
    }
  }

  public void incrementActiveWorkers() {
    activeWorkers.incrementAndGet();
  }

  public void decrementActiveWorkers() {
    activeWorkers.decrementAndGet();
  }

  @Override
  public void accept(FlowInfo flowInfo) {
    Status status = flowInfo.getStatus();
    messagesTotal.get(status).increment();
    messagesSeconds.get(status).increment(flowInfo.duration().getSeconds());
  }
}
