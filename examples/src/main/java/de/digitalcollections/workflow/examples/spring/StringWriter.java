package de.digitalcollections.workflow.examples.spring;

import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class StringWriter implements Function<String, Message> {

  @Override
  public DefaultMessage apply(String s) {
    return DefaultMessage.withType("uppercase-strings").put("text", s);
  }

}
