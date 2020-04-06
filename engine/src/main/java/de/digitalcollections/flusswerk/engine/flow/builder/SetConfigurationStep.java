package de.digitalcollections.flusswerk.engine.flow.builder;

import de.digitalcollections.flusswerk.engine.flow.Flow;
import de.digitalcollections.flusswerk.engine.flow.FlowStatus;
import de.digitalcollections.flusswerk.engine.model.Message;
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
   * Sets a process monitor that consumes a {@link FlowStatus} instance every time a message has
   * been processed. builder step.
   *
   * @param m the process monitor
   * @return the next step (setting configuration or build the flow)
   */
  public SetConfigurationStep<M, R, W> monitor(Consumer<FlowStatus> m) {
    model.setMonitor(m);
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
        model.getMonitor());
  }
}
