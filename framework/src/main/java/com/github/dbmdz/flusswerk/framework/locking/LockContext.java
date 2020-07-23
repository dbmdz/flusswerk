package com.github.dbmdz.flusswerk.framework.locking;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

class LockContext {

  private final Lock lock;
  private final String id;
  private long acquired;
  private long acquisitionStarted;
  private long released;
  private final Watch watch;

  public LockContext(Lock lock, String id) {
    this(lock, id, new SystemWatch());
  }

  public LockContext(Lock lock, String id, Watch watch) {
    this.lock = requireNonNull(lock);
    this.id = id;
    this.acquired = -1;
    this.acquisitionStarted = -1;
    this.released = -1;
    this.watch = watch;
  }

  public void acquire(long timeout, TimeUnit unit) throws InterruptedException {
    acquisitionStarted = watch.now();
    lock.tryLock(timeout, unit);
    acquired = watch.now();
  }

  public void release() {
    lock.unlock();
    released = watch.now();
  }

  public long waitedForAcquisitionNs() {
    if (acquisitionStarted == -1 || acquired == -1) {
      return 0; // Lock never has been acquired
    }
    return acquired - acquisitionStarted;
  }

  public long lockHeldNs() {
    if (acquired == -1 || released == -1) {
      return 0; // Lock has never been held
    }
    return released - acquired;
  }

  public String getId() {
    return id;
  }
}
