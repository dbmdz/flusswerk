package com.github.dbmdz.flusswerk.framework.locking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.mockito.stubbing.OngoingStubbing;
import org.mockito.verification.VerificationMode;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

class LockingFixture {

  private final RedissonClient redissonClient;
  private final RedisLockManager redisLockManager;
  private final long timeoutMs;

  public LockingFixture(
      RedissonClient redissonClient, RedisLockManager redisLockManager, long timeoutMs) {
    this.redissonClient = redissonClient;
    this.redisLockManager = redisLockManager;
    this.timeoutMs = timeoutMs;
  }

  Lock mockLockForId(String id) {
    var lock = mock(RLock.class);
    when(redissonClient.getLock(redisLockManager.key(id))).thenReturn(lock);
    return lock;
  }

  Lock mockLockForAnyId() {
    var lock = mock(RLock.class);
    when(redissonClient.getLock(any())).thenReturn(lock);
    return lock;
  }

  public OngoingStubbing<Boolean> whenTryAcquire(Lock lock) throws InterruptedException {
    return when(lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS));
  }

  public void verifyLockHasBeenAcquired(Lock lock) throws InterruptedException {
    verify(lock).tryLock(timeoutMs, TimeUnit.MILLISECONDS);
  }

  public void verifyLockHasBeenAcquired(Lock lock, VerificationMode mode)
      throws InterruptedException {
    verify(lock, mode).tryLock(timeoutMs, TimeUnit.MILLISECONDS);
  }
}
