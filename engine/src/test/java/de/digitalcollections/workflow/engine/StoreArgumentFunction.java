package de.digitalcollections.workflow.engine;

import java.util.function.Function;

public class StoreArgumentFunction<R, T> implements Function<R, T> {

  private R value;

  @Override
  public T apply(R argument) {
    value = argument;
    return null;
  }

  public R getValue() {
    return value;
  }

}
