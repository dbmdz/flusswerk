package de.digitalcollections.flusswerk.engine.flow.builder;

import java.util.function.Consumer;
import org.assertj.core.api.Condition;

class InvocationProbe<T> implements Consumer<T>, Runnable {

  private int invocations = 0;

  @Override
  public void accept(T s) {
    invocations++;
  }

  @Override
  public void run() {
    invocations++;
  }

  public boolean hasBeenInvoked() {
    return invocations > 0;
  }

  public int getInvocations() {
    return invocations;
  }

  public static Condition<? super InvocationProbe<?>> beenInvoked() {
    return new Condition<>(InvocationProbe::hasBeenInvoked, "at least one invocation");
  }
}
