package com.github.dbmdz.flusswerk.framework.flow;

import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.locking.LockManager;
import java.util.Optional;
import java.util.function.Function;

public abstract class LockingFunction<T, R> implements Function<T, R> {

  private LockManager lockManager;

  void setLockManager(LockManager lockManager) {
    this.lockManager = lockManager;
  }

  public void lock(String id) {
    try {
      lockManager.acquire(id);
    } catch (InterruptedException e) {
      throw new StopProcessingException("Could not acquire lock for id {}", id).causedBy(e);
    }
  }

  public void release() {
    lockManager.release();
  }

  public boolean hasLock() {
    return lockManager.threadHasLock();
  }

  public Optional<String> lockedId() {
    return lockManager.getLockedIdForThread();
  }
}
