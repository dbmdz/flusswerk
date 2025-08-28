package dev.mdz.flusswerk.flow.builder;

import static java.util.Collections.emptyList;

import dev.mdz.flusswerk.model.Message;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Set a new message processor that takes a message and returns a {@link List} of messages.
 *
 * @param <M> The message class
 */
public class MessageProcessorStep<M extends Message> {

  private final Model<M, M, M> model;

  MessageProcessorStep(Model<M, M, M> model) {
    this.model = model;
  }

  /**
   * Set a message processor that receives a message of type <code>M</code> and returns a {@link
   * java.util.Collection} of {@link Message}, then moves you to the next builder step.
   *
   * @param p the message processor to set
   * @return the next reader step
   */
  public ConfigurationStep<M, M, M> expand(Function<M, Collection<Message>> p) {
    model.setReader(m -> m);
    model.setTransformer(m -> m);
    model.setWriter(p);
    return new ConfigurationStep<>(model);
  }

  /**
   * Set a message processor that receives a message of type <code>M</code> and returns an arbitrary
   * {@link Message}, then moves you to the next builder step.
   *
   * @param p the message processor to set
   * @return the next reader step
   */
  public ConfigurationStep<M, M, M> process(Function<M, Message> p) {
    model.setReader(m -> m);
    model.setTransformer(m -> m);
    model.setWriter(p.andThen(m -> (m == null) ? emptyList() : List.of(m)));
    return new ConfigurationStep<>(model);
  }

  /**
   * Set a message processor that receives a message of type <code>M</code> and does not return any
   * {@link Message} for Flusswerk to send.
   *
   * @param consumer the message processor to set
   * @return the next reader step
   */
  public ConfigurationStep<M, M, M> consume(Consumer<M> consumer) {
    model.setReader(m -> m);
    model.setTransformer(m -> m);
    model.setWriter(
        m -> {
          consumer.accept(m);
          return emptyList();
        });
    return new ConfigurationStep<>(model);
  }
}
