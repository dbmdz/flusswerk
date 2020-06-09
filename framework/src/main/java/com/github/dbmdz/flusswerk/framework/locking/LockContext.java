package com.github.dbmdz.flusswerk.framework.locking;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public class LockContext {

  private final Lock lock;

  private final String id;

  private Instant acquired;

  private Instant acquisitionStarted;

  private Instant released;

  public LockContext(Lock lock, String id) {
    this.lock = requireNonNull(lock);
    this.id = id;
    this.acquired = null;
    this.acquisitionStarted = null;
    this.released = null;
  }

  public void acquire(long timeout, TimeUnit unit) throws InterruptedException {
    acquisitionStarted = Instant.now();
    lock.tryLock(timeout, unit);
    acquired = Instant.now();
  }

  public void release() {
    lock.unlock();
    released = Instant.now();
  }

  public Duration waitedForAcquisition() {
    if (acquisitionStarted == null || acquired == null) {
      return Duration.ZERO; // Lock never has been acquired
    }
    return Duration.between(acquisitionStarted, acquired);
  }

  public Duration lockHeld() {
    if (acquired == null || released == null) {
      return Duration.ZERO; // Lock has never been held
    }
    return Duration.between(acquired, released);
  }

  public String getId() {
    return id;
  }
}
