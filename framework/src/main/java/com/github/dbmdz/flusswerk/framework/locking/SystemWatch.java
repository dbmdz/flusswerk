package com.github.dbmdz.flusswerk.framework.locking;

public class SystemWatch implements Watch {

  @Override
  public long now() {
    return System.nanoTime();
  }
}
