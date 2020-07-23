package com.github.dbmdz.flusswerk.framework.locking;

import java.util.Optional;

public interface LockManager {

  void acquire(String id) throws InterruptedException;

  void release();

  /** @return the total number of successfully acquired locks */
  long getLocksAcquired();

  /**
   * @return the total amount of time spent on waiting for locks. Time for locks held at the same
   *     time is added up.
   */
  long getWaitedForLocksNs();

  /**
   * @return the total amount of time all locks have been held. Time for locks held at the same time
   *     is added up.
   */
  long getLocksHeldNs();

  boolean threadHasLock();

  Optional<String> getLockedIdForThread();
}
