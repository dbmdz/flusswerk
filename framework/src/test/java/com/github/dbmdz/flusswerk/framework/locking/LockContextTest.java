package com.github.dbmdz.flusswerk.framework.locking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

@DisplayName("The LockContext")
class LockContextTest {

  private TestingWatch watch;
  private Lock lock;
  private LockContext lockContext;

  @BeforeEach
  void setUp() {
    lock = mock(Lock.class);
    watch = new TestingWatch();
    lockContext = new LockContext(lock, "123", watch);
  }

  @DisplayName("should acquire locks")
  @Test
  void acquire() throws InterruptedException {
    var timeout = 50;
    var timeUnit = TimeUnit.MILLISECONDS;
    lockContext.acquire(timeout, timeUnit);
    verify(lock).tryLock(timeout, timeUnit);
  }

  @DisplayName("should release lock")
  @Test
  void release() {
    lockContext.release();
    verify(lock).unlock();
  }

  @DisplayName("should record the correct time waited for lock acquisition")
  @Test
  void waitedForAcquisition() throws InterruptedException {
    var expected = 30;

    when(lock.tryLock(50, TimeUnit.MILLISECONDS))
        .then(
            (Answer<Boolean>)
                invocation -> {
                  watch.sleepNano(expected);
                  return true;
                });

    lockContext.acquire(50, TimeUnit.MILLISECONDS);

    assertThat(lockContext.waitedForAcquisitionNs()).isEqualTo(expected);
  }

  @DisplayName("should return no time waited for locks never acquired")
  @Test
  void shouldReturnNoTimeWaitedForLocksNeverAcquired() throws InterruptedException {
    watch.sleepNano(123); // wait some time to rule out accidental measurements
    lockContext.acquire(50, TimeUnit.MILLISECONDS);

    assertThat(lockContext.waitedForAcquisitionNs()).isZero();
  }

  @DisplayName("should return no time waited for locks never acquired")
  @Test
  void shouldReturnNoTimeHeldForLocksNeverAcquired() throws InterruptedException {
    watch.sleepNano(123); // wait some time to rule out accidental measurements
    assertThat(lockContext.lockHeldNs()).isZero();
  }

  @DisplayName("should record the correct time the lock has been held")
  @Test
  void lockHeld() throws InterruptedException {

    when(lock.tryLock(50, TimeUnit.MILLISECONDS))
        .then(
            (Answer<Boolean>)
                invocation -> {
                  watch.sleepNano(100); // wait for lock to make sure the correct time is measured
                  return true;
                });

    var expected = 100;

    lockContext.acquire(50, TimeUnit.MILLISECONDS);
    watch.sleepNano(expected); // wait time to release lock
    lockContext.release();

    assertThat(lockContext.lockHeldNs()).isEqualTo(expected);
  }

  @DisplayName("should return the id")
  @Test
  void getId() {
    assertThat(lockContext.getId()).isEqualTo("123");
  }
}
