package com.github.dbmdz.flusswerk.framework.flow.builder;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Set a new writer while building the new flow (typesafe).
 *
 * @param <M> The message class
 * @param <R> Generic type for the reader output/transformer input
 * @param <W> Generic type for the transformer output/writer input
 */
public class SetWriterStep<M extends Message, R, W> {

  private Model<M, R, W> model;

  public SetWriterStep(Model<M, R, W> model) {
    this.model = model;
  }

  /**
   * Sets a writer that receives data of type and stops processing, then moves you to the next
   * builder step.
   *
   * @param w the writer to set
   * @return the next step (setting configuration or build the flow)
   */
  public SetConfigurationStep<M, R, W> writerSendingNothing(Consumer<W> w) {
    model.setWriter(
        item -> {
          w.accept(item);
          return Collections.emptyList();
        });
    return new SetConfigurationStep<>(model);
  }

  /**
   * Sets a writer that receives data of type and sends a new message, then moves you to the next
   * builder step.
   *
   * @param w the writer to set
   * @return the next step (setting configuration or build the flow)
   */
  public SetConfigurationStep<M, R, W> writerSendingMessage(Function<W, Message> w) {
    model.setWriter(
        item -> {
          var result = w.apply(item);
          if (result == null) {
            return Collections.emptyList();
          } else {
            return List.of(result);
          }
        });
    return new SetConfigurationStep<>(model);
  }

  /**
   * Sets a writer that receives data of type and sends a {@link List} of new messages, then moves
   * you to the next builder step.
   *
   * @param w the writer to set
   * @return the next step (setting configuration or build the flow)
   */
  public SetConfigurationStep<M, R, W> writerSendingMessages(Function<W, Collection<Message>> w) {
    model.setWriter(w);
    return new SetConfigurationStep<>(model);
  }
}
