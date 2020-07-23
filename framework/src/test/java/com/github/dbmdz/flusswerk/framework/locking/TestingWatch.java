package com.github.dbmdz.flusswerk.framework.locking;

public class TestingWatch implements Watch {

  private long now;

  public TestingWatch() {
    this.now = 333; // do not start at zero to rule out no-ops in measurements
  }

  @Override
  public long now() {
    return now;
  }

  public void sleepNano(long time) {
    now += time;
  }
}
