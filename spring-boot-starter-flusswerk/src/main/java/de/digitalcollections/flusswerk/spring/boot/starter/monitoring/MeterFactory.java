package de.digitalcollections.flusswerk.spring.boot.starter.monitoring;

import de.digitalcollections.flusswerk.engine.flow.FlowStatus.Status;
import de.digitalcollections.flusswerk.spring.boot.starter.FlusswerkProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Convenience factory to simplify the creation of {@link Counter} meters. */
@Component
public class MeterFactory {
  private String basename;
  private String app;
  private MeterRegistry registry;

  /**
   * @param flusswerkProperties The properties containing the prefix for the created metrics ("e.g.
   *     workflow.job for workflow.job.items.total"
   * @param app The app name to add as a tag to all metrics
   * @param registry the Micrometer registry to create the counters
   */
  public MeterFactory(
      FlusswerkProperties flusswerkProperties,
      @Value("spring.application.name") String app,
      MeterRegistry registry) {
    this.basename = flusswerkProperties.getConnection().getConnectTo();
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
