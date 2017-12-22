package de.digitalcollections.workflow.examples.spring;

import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class UppercaseTransformer implements Function<String, String> {

  @Override
  public String apply(String s) {
    if (s == null) {
      return null;
    }
    return s.toUpperCase();
  }

}
