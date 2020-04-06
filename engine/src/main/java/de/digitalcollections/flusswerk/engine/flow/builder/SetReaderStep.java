package de.digitalcollections.flusswerk.engine.flow.builder;

import de.digitalcollections.flusswerk.engine.model.Message;
import java.util.function.Function;

/**
 * Set a new reader while building the new flow (typesafe).
 *
 * @param <M> The message class
 * @param <R> Generic type for the reader output/transformer input
 * @param <W> Generic type for the transformer output/writer input
 */
public class SetReaderStep<M extends Message, R, W> {

  private Model<M, R, W> model;

  SetReaderStep(Model<M, R, W> model) {
    this.model = model;
  }

  /**
   * Sets a reader that receives a message of type <code>M</code> and creates a model object of type
   * <code>R</code>, then moves you to the next builder step.
   *
   * @param r the reader to set
   * @return the next reader step
   */
  public SetTransformerStep<M, R, W> reader(Function<M, R> r) {
    model.setReader(r);
    return new SetTransformerStep<>(model);
  }
}
