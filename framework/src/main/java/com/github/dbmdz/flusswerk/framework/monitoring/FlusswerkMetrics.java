package com.github.dbmdz.flusswerk.framework.monitoring;

import com.github.dbmdz.flusswerk.framework.config.properties.ProcessingProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.concurrent.atomic.AtomicInteger;

public class FlusswerkMetrics {

  private final AtomicInteger activeWorkers = new AtomicInteger(0);

  private final int totalWorkers;

  public FlusswerkMetrics(ProcessingProperties properties, MeterRegistry registry) {
    this.totalWorkers = properties.getThreads();

    Gauge.builder("flusswerk.workers.total", activeWorkers, AtomicInteger::get)
        .description("Number of worker threads in the system")
        .tags(Tags.of("state", "active"))
        .register(registry);

    Gauge.builder("flusswerk.workers.total", activeWorkers, w -> totalWorkers - w.get())
        .description("Number of worker threads in the system")
        .tags(Tags.of("state", "idle"))
        .register(registry);
  }

  public void incrementActiveWorkers() {
    activeWorkers.incrementAndGet();
  }

  public void decrementActiveWorkers() {
    activeWorkers.decrementAndGet();
  }
}
