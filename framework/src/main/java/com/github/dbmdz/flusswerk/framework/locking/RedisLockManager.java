package com.github.dbmdz.flusswerk.framework.locking;

import java.util.Optional;

public class RedisLockManager implements LockManager {

  @Override
  public void acquire(String id) throws InterruptedException {
    // TODO
  }

  @Override
  public void release() {
    // TODO
  }

  @Override
  public long getLocksAcquired() {
    return 0;
  }

  @Override
  public long getWaitedForLocksMs() {
    return 0;
  }

  @Override
  public long getLocksHeldMs() {
    return 0;
  }

  @Override
  public boolean threadHasLock() {
    return false;
  }

  @Override
  public Optional<String> getLockedIdForThread() {
    return Optional.empty();
  }
}
