package com.github.dbmdz.flusswerk.framework.monitoring;

import com.github.dbmdz.flusswerk.framework.config.properties.FlusswerkProperties;
import com.github.dbmdz.flusswerk.framework.flow.FlowInfo.Status;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

/** Convenience factory to simplify the creation of {@link Counter} meters. */
public class MeterFactory {
  private final String basename;
  private final String app;
  private final MeterRegistry registry;

  /**
   * @param flusswerkProperties The properties containing the prefix for the created metrics ("e.g.
   *     workflow.job for workflow.job.items.total"
   * @param app The app name to add as a tag to all metrics
   * @param registry the Micrometer registry to create the counters
   */
  public MeterFactory(
      FlusswerkProperties flusswerkProperties,
      @Value("${spring.application.name}") String app,
      MeterRegistry registry) {
    this.basename = flusswerkProperties.getMonitoring().getPrefix();
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
    List<String> allTags = new ArrayList<>(Arrays.asList(tags));
    allTags.add("status");
    allTags.add(status.toString().toLowerCase());
    return counter(metric, allTags.toArray(new String[]{}));
  }

}
