package de.digitalcollections.flusswerk.examples.spring;

import de.digitalcollections.flusswerk.engine.model.DefaultMessage;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class StringReader implements Function<DefaultMessage, String> {

  @Override
  public String apply(DefaultMessage defaultMessage) {
    return defaultMessage.get("text");
  }

}
