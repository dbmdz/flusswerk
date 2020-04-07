package de.digitalcollections.flusswerk.engine.flow.builder;

import de.digitalcollections.flusswerk.engine.model.Message;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Set a new message processor that takes a message and returns a {@link List} of messages.
 *
 * @param <M> The message class
 */
public class SetMessageProcessorStep<M extends Message> {

  private Model<M, M, M> model;

  SetMessageProcessorStep(Model<M, M, M> model) {
    this.model = model;
  }

  /**
   * Set a message processor that receives a message of type <code>M</code> and returns a {@link
   * java.util.Collection} of {@link Message}, then moves you to the next builder step.
   *
   * @param p the message processor to set
   * @return the next reader step
   */
  public SetConfigurationStep<M, M, M> expand(Function<M, Collection<Message>> p) {
    model.setReader(m -> m);
    model.setTransformer(m -> m);
    model.setWriter(p);
    return new SetConfigurationStep<>(model);
  }


  /**
   * Set a message processor that receives a message of type <code>M</code> and returns an arbitrary
   * {@link Message}, then moves you to the next builder step.
   *
   * @param p the message processor to set
   * @return the next reader step
   */
  public SetConfigurationStep<M, M, M> process(Function<M, Message> p) {
    model.setReader(m -> m);
    model.setTransformer(m -> m);
    model.setWriter(List::of);
    return new SetConfigurationStep<>(model);
  }
}
