package com.github.dbmdz.flusswerk.framework.flow.builder;

import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.flow.FlowStatus;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.function.Consumer;

/**
 * Set configuration for the new flow and build it.
 *
 * @param <M> The message class
 * @param <R> Generic type for the reader output/transformer input
 * @param <W> Generic type for the transformer output/writer input
 */
public class SetConfigurationStep<M extends Message, R, W> {

  private Model<M, R, W> model;

  SetConfigurationStep(Model<M, R, W> model) {
    this.model = model;
  }

  /**
   * Sets a process metrics monitor that consumes a {@link FlowStatus} instance every time a message
   * has been processed. builder step.
   *
   * @param m the process metrics monitor
   * @return the next step (setting configuration or build the flow)
   */
  public SetConfigurationStep<M, R, W> metrics(Consumer<FlowStatus> m) {
    model.setMetrics(m);
    return this;
  }

  /**
   * Sets a cleanup task process monitor that consumes a {@link FlowStatus} instance every time a
   * message has been processed. builder step.
   *
   * @param c the cleanup task
   * @return the next step (setting configuration or build the flow)
   */
  public SetConfigurationStep<M, R, W> cleanup(Runnable c) {
    model.setCleanup(c);
    return this;
  }

  /**
   * Sets if flow ids should be propagated from input to output messages. This will be replaced by a
   * more automatic process in the next Flusswerk version and is here for compatibility reasons
   * only.
   *
   * @param p true, if flow ids should be propagated
   * @return the next step (setting configuration or build the flow)
   */
  public SetConfigurationStep<M, R, W> propagateFlowIds(boolean p) {
    model.setPropagateFlowIds(p);
    return this;
  }

  /**
   * Build the new flow.
   *
   * @return the new flow
   */
  public Flow<M, R, W> build() {
    // Suppliers are due to the Flow constructor interface. Suppliers are likely to be dropped in
    // Flusswerk 4.
    return new Flow<>(
        () -> model.getReader(),
        () -> model.getTransformer(),
        () -> model.getWriter(),
        null, // for cleaner implementation, adapting different writer types is completely done in
        // the builder
        model.getCleanup(),
        model.isPropagateFlowIds(),
        model.getMetrics());
  }
}
