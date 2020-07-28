package com.github.dbmdz.flusswerk.integration;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.function.Function;

/**
 * Adapter class so that tests can inject test logic while we still can use Spring Test Autowiring
 * setup.
 */
public class ProcessorAdapter implements Function<Message, Message> {

  private Function<Message, Message> function;

  @Override
  public Message apply(Message message) {
    if (function == null) {
      throw new RuntimeException("Processor called before actual function was assigned");
    }
    return function.apply(message);
  }

  public void setFunction(Function<Message, Message> function) {
    this.function = function;
  }
}
