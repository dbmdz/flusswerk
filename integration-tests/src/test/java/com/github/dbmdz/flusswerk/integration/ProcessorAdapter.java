package com.github.dbmdz.flusswerk.integration;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.function.Function;

/**
 * Adapter class so that tests can inject test logic while we still can use Spring Test Autowiring
 * setup.
 */
public class ProcessorAdapter<M extends Message> implements Function<M, Message> {

  private Function<M, Message> function;

  @Override
  public Message apply(M message) {
    if (function == null) {
      throw new RuntimeException("Processor called before actual function was assigned");
    }
    return function.apply(message);
  }

  public void setFunction(Function<M, Message> function) {
    this.function = function;
  }
}
