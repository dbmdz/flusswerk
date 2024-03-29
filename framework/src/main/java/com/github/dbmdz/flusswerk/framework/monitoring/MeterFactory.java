package com.github.dbmdz.flusswerk.framework.monitoring;

import static java.util.Arrays.asList;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;

/** Convenience factory to simplify the creation of {@link Counter} meters. */
public class MeterFactory {
  private final String basename;
  private final MeterRegistry registry;

  /**
   * @param basename The prefix for the created metrics ("e.g. flusswerk for flusswerk.items.total")
   * @param registry the Micrometer registry to create the counters
   */
  public MeterFactory(String basename, MeterRegistry registry) {
    this.basename = basename;
    this.registry = registry;
  }

  public Counter counter(String metric, String... tags) {
    var completeName = basename + "." + metric;
    return registry.counter(completeName, tags);
  }

  public Counter counter(String metric, Status status, String... tags) {
    List<String> allTags = new ArrayList<>(asList(tags));
    allTags.add("status");
    allTags.add(status.toString().toLowerCase());
    return counter(metric, allTags.toArray(new String[] {}));
  }

  public Counter frameworkCounter(String name, Status status) {
    return registry.counter("flusswerk." + name, "status", status.toString().toLowerCase());
  }
}
