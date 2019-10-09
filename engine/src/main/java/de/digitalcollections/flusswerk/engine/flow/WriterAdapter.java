package de.digitalcollections.flusswerk.engine.flow;

import de.digitalcollections.flusswerk.engine.model.Message;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public class WriterAdapter<T> implements Function<T, Collection<Message>> {

  private final Function<T, Message> writer;

  public WriterAdapter(Function<T, Message> writer) {
    this.writer = writer;
  }

  @Override
  public Collection<Message> apply(T t) {
    Message result = writer.apply(t);
    if (result == null) {
      return Collections.emptyList();
    } else {
      return Collections.singletonList(result);
    }
  }
}
