package dev.mdz.flusswerk.flow;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import dev.mdz.flusswerk.model.Message;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public record FlowSpec(
    Function<Message, Object> reader,
    Function<Object, Object> transformer,
    Function<Object, Collection<Message>> writer,
    Runnable cleanup,
    Consumer<FlowInfo> monitor) {

  public FlowSpec(
      Function<Message, Object> reader,
      Function<Object, Object> transformer,
      Function<Object, Collection<Message>> writer,
      Runnable cleanup,
      Consumer<FlowInfo> monitor) {
    this.reader = requireNonNull(reader);
    this.transformer = requireNonNull(transformer);
    this.writer = requireNonNull(writer);
    this.cleanup = requireNonNullElse(cleanup, () -> {});
    this.monitor = requireNonNullElse(monitor, metrics -> {});
  }
}
