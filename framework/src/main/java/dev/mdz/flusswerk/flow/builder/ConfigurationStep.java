package dev.mdz.flusswerk.flow.builder;

import dev.mdz.flusswerk.flow.FlowInfo;
import dev.mdz.flusswerk.flow.FlowSpec;
import dev.mdz.flusswerk.model.Message;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Set configuration for the new flow and build it.
 *
 * @param <M> The message class
 * @param <R> Generic type for the reader output/transformer input
 * @param <W> Generic type for the transformer output/writer input
 */
public class ConfigurationStep<M extends Message, R, W> {

  private final Model<M, R, W> model;

  ConfigurationStep(Model<M, R, W> model) {
    this.model = model;
  }

  /**
   * Sets a process metrics monitor that consumes a {@link FlowInfo} instance every time a message
   * has been processed. builder step.
   *
   * @param m the process metrics monitor
   * @return the next step (setting configuration or build the flow)
   */
  public ConfigurationStep<M, R, W> metrics(Consumer<FlowInfo> m) {
    model.setMetrics(m);
    return this;
  }

  /**
   * Sets a cleanup task process monitor that consumes a {@link FlowInfo} instance every time a
   * message has been processed. builder step.
   *
   * @param c the cleanup task
   * @return the next step (setting configuration or build the flow)
   */
  public ConfigurationStep<M, R, W> cleanup(Runnable c) {
    model.setCleanup(c);
    return this;
  }

  /**
   * Build the new flow.
   *
   * @return the new flow
   */
  @SuppressWarnings("unchecked")
  public FlowSpec build() {
    return new FlowSpec(
        (Function<Message, Object>) model.getReader(),
        (Function<Object, Object>) model.getTransformer(),
        (Function<Object, Collection<Message>>) model.getWriter(),
        model.getCleanup(),
        model.getMetrics());
  }
}
