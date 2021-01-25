package com.github.dbmdz.flusswerk.framework.locking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.dbmdz.flusswerk.framework.exceptions.LockingException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.Answer;
import org.redisson.api.RedissonClient;

@DisplayName("The RedisLockManager")
class RedisLockManagerTest {

  private static final Duration TIMEOUT = Duration.ofMillis(50);

  private TestingWatch testingWatch;
  private RedissonClient redissonClient;
  private RedisLockManager redisLockManager;
  private LockingFixture lockingFixture;

  @BeforeEach
  void setUp() {
    redissonClient = mock(RedissonClient.class);
    testingWatch = new TestingWatch();
    redisLockManager = new RedisLockManager(redissonClient, "flusswerk", TIMEOUT, testingWatch);
    lockingFixture = new LockingFixture(redissonClient, redisLockManager, TIMEOUT.toMillis());
  }

  @DisplayName("should use key space for keys")
  @Test
  void shouldUseKeySpaceForKeys() {
    assertThat(redisLockManager.key("123")).isEqualTo("flusswerk::123");
  }

  @DisplayName("should block if lock for id already has been acquired")
  @Test
  void shouldBlockIfLockForIdHasAlreadyBeenAcquired() {
    var id = "123";
    lockingFixture.mockLockForId(id);

    redisLockManager.acquire(id, 1); // simulate different threads
    redisLockManager.acquire(id, 2); // using fake thread ids

    verify(redissonClient, times(2)).getLock(redisLockManager.key(id));
  }

  @DisplayName("should throw exception if the same thread tries to acquire more than one lock")
  @Test
  void shouldThrowExceptionIfTheSameThreadTriesToAcquireMoreThanOneLock() {
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(
            () -> {
              redisLockManager.acquire("123");
              redisLockManager.acquire("456");
            });
  }

  @DisplayName("should acquire lock with timeout")
  @Test
  void shouldAcquireLockWithTimeOut() throws InterruptedException {
    var id = "123";
    var lock = lockingFixture.mockLockForId(id);
    redisLockManager.acquire(id);
    lockingFixture.verifyLockHasBeenAcquired(lock);
  }

  @DisplayName("should forget locks if acquiring fails")
  @Test
  void shouldForgetLocksIfAcquiringFails() throws InterruptedException {
    var id = "123";
    var lock = lockingFixture.mockLockForId(id);

    lockingFixture.whenTryAcquire(lock).thenThrow(InterruptedException.class);

    assertThatExceptionOfType(LockingException.class)
        .isThrownBy(() -> redisLockManager.acquire(id));
    assertThat(redisLockManager.getLockedIdForThread()).isEmpty();
  }

  @DisplayName("should release lock")
  @Test
  void shouldReleaseLock() {
    lockingFixture.mockLockForAnyId();
    redisLockManager.acquire("123");
    assertThat(redisLockManager.threadHasLock()).isTrue();
    redisLockManager.release();
    assertThat(redisLockManager.threadHasLock()).isFalse();
  }

  @DisplayName("should silently do nothing if there is nothing to release")
  @Test
  void shouldSilentlyDoNothingIfThereIsNothingToRelease() {
    redisLockManager.release();
  }

  @DisplayName("should return the total number of locks acquired")
  @Test
  void shouldReturnTheTotalAmountOfAcquiredLocks() {
    lockingFixture.mockLockForAnyId();

    var expected = 3;
    for (int threadId = 0; threadId < expected; threadId++) {
      var id = Integer.toString(threadId);
      redisLockManager.acquire(id, threadId); // simulate multiple threads
    }
    assertThat(redisLockManager.getLocksAcquired()).isEqualTo(expected);
  }

  @DisplayName("should display the total amount of time waited for locks")
  @Test
  void getWaitedForLocksNs() throws InterruptedException {
    var lock = lockingFixture.mockLockForAnyId();

    var waitingTime = 500_0000;

    lockingFixture
        .whenTryAcquire(lock)
        .then(
            (Answer<Boolean>)
                invocation -> {
                  testingWatch.sleepNano(waitingTime);
                  return true;
                });

    var expected = 2 * waitingTime;
    redisLockManager.acquire("123");
    redisLockManager.release();
    redisLockManager.acquire("32123");
    redisLockManager.release();

    assertThat(redisLockManager.getWaitedForLocksNs()).isEqualTo(expected);
  }

  @Test
  void getLocksHeldNs() {
    lockingFixture.mockLockForAnyId();

    var waitingTime = 500_0000;
    var expected = 2 * waitingTime;
    redisLockManager.acquire("123");
    testingWatch.sleepNano(waitingTime);
    redisLockManager.release();

    redisLockManager.acquire("32123");
    testingWatch.sleepNano(waitingTime);
    redisLockManager.release();

    assertThat(redisLockManager.getLocksHeldNs()).isEqualTo(expected);
  }

  @DisplayName("should report if thread has lock")
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldReturnIfThreadHasLocks(boolean acquireLock) {
    lockingFixture.mockLockForAnyId();
    if (acquireLock) {
      redisLockManager.acquire("123");
    }
    assertThat(redisLockManager.threadHasLock()).isEqualTo(acquireLock);
  }

  @DisplayName("should return the id for the threads current lock")
  @Test
  void shouldReturnTheIdForTheThreadsCurrentLock() {
    lockingFixture.mockLockForAnyId();
    var expected = "123";
    redisLockManager.acquire(expected);
    assertThat(redisLockManager.getLockedIdForThread()).contains(expected);
  }

  @DisplayName("should return empty id if current thread has no lock")
  @Test
  void shouldReturnEmptyIdIfCurrentThreadHasNoLock() {
    assertThat(redisLockManager.getLockedIdForThread()).isEmpty();
  }
}
