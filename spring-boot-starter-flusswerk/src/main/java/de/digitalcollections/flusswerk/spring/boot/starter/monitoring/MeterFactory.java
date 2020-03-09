package de.digitalcollections.flusswerk.spring.boot.starter.monitoring;

import de.digitalcollections.flusswerk.engine.flow.FlowStatus.Status;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Convenience factory to simplify the creation of {@link Counter} meters. */
public class MeterFactory {
  private String basename;
  private String app;
  private MeterRegistry registry;

  public MeterFactory(String basename, String app, MeterRegistry registry) {
    this.basename = basename;
    this.app = app;
    this.registry = registry;
  }

  public Counter counter(String metric, String... tags) {
    var completeName = basename + "." + metric;
    List<String> allTags = new ArrayList<>();
    allTags.addAll(List.of("job", app));
    allTags.addAll(Arrays.asList(tags));
    return registry.counter(completeName, allTags.toArray(new String[] {}));
  }

  public Counter counter(String metric, Status status, String... tags) {
    return counter(metric, "status", status.toString().toLowerCase());
  }
}
