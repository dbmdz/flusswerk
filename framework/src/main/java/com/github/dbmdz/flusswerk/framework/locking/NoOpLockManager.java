package com.github.dbmdz.flusswerk.framework.locking;

import com.github.dbmdz.flusswerk.framework.exceptions.LockingException;
import java.util.Optional;

public class NoOpLockManager implements LockManager {

  @Override
  public void acquire(String id) throws LockingException {
    throw new RuntimeException("Cannot acquire locks. This is the noop version.");
  }

  @Override
  public void release() {}

  @Override
  public long getLocksAcquired() {
    return 0;
  }

  @Override
  public long getWaitedForLocksNs() {
    return 0;
  }

  @Override
  public long getLocksHeldNs() {
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

  @Override
  public boolean isLocked(String id) {
    return false;
  }
}
