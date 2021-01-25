package com.github.dbmdz.flusswerk.framework.locking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SystemWatchTest {

  @Test
  void now() {
    var watch = new SystemWatch();
    var before = System.nanoTime();
    var actual = watch.now();
    var after = System.nanoTime();
    assertThat(actual).isBetween(before, after);
  }
}
