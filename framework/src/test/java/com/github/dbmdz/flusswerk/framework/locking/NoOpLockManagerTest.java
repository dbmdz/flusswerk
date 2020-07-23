package com.github.dbmdz.flusswerk.framework.locking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The NoOpLockManager")
class NoOpLockManagerTest {

  private NoOpLockManager noOpLockManager;

  @BeforeEach
  void setUp() {
    noOpLockManager = new NoOpLockManager();
  }

  @DisplayName("should throw Exception if lock should be acquired")
  @Test
  void acquire() {
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> noOpLockManager.acquire("123"));
  }

  @DisplayName("should release silently the non-existent locks")
  @Test
  void release() {
    noOpLockManager.release();
  }

  @DisplayName("should report correct number of acquired locks (always zero)")
  @Test
  void getLocksAcquired() {
    assertThat(noOpLockManager.getLocksAcquired()).isZero();
  }

  @DisplayName("should report correct time that has been waited for locks (always zero)")
  @Test
  void getWaitedForLocksMs() {
    assertThat(noOpLockManager.getWaitedForLocksNs()).isZero();
  }

  @DisplayName("should report correct time that locks had been held (always zero)")
  @Test
  void getLocksHeldMs() {
    assertThat(noOpLockManager.getLocksHeldNs()).isZero();
  }

  @DisplayName("should report that no locks are held (acquiring locks can't happen)")
  @Test
  void threadHasLock() {
    assertThat(noOpLockManager.threadHasLock()).isFalse();
  }

  @DisplayName("should not return locked ids for threads")
  @Test
  void getLockedIdForThread() {
    assertThat(noOpLockManager.getLockedIdForThread()).isEmpty();
  }
}
