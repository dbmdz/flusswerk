package com.github.dbmdz.flusswerk.framework.locking;

import java.util.Optional;

public class NoOpLockManager implements LockManager {

  @Override
  public void acquire(String id) throws InterruptedException {
    throw new RuntimeException("Cannot acquire locks. This is the noop version.");
  }

  @Override
  public void release() {}

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
