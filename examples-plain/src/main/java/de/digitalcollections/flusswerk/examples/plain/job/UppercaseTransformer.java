package de.digitalcollections.flusswerk.examples.plain.job;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Transform a string to uppercase. */
public class UppercaseTransformer implements Function<String, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(UppercaseTransformer.class);

  private final boolean transformationShouldBeSlow;

  public UppercaseTransformer() {
    this.transformationShouldBeSlow = false;
  }

  public UppercaseTransformer(boolean transformationShouldBeSlow) {
    this.transformationShouldBeSlow = transformationShouldBeSlow;
  }

  @Override
  public String apply(String s) {
    if (transformationShouldBeSlow) {
      sleepSomeTime();
    }
    return s.toUpperCase();
  }

  private void sleepSomeTime() {
    try {
      TimeUnit.MILLISECONDS.sleep(250);
    } catch (InterruptedException e) {
      LOGGER.error("Sleep interrupted...", e);
    }
  }
}
