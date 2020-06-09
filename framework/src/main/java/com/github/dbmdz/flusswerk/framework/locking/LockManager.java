package com.github.dbmdz.flusswerk.framework.locking;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class LockManager {

  private final RedissonClient client;
  private final ConcurrentMap<Long, LockContext> locks;
  private final String keyspace;
  private final long timeout;
  private final AtomicLong locksAcquired;
  private final AtomicLong waitedForLocksMs;
  private final AtomicLong locksHeldMs;

  public LockManager(RedissonClient client, String keyspace, Duration timeout) {
    this.client = requireNonNull(client);
    this.keyspace = requireNonNull(keyspace);
    this.timeout = timeout.toMillis();
    this.locks = new ConcurrentHashMap<>();
    locksAcquired = new AtomicLong();
    waitedForLocksMs = new AtomicLong();
    locksHeldMs = new AtomicLong();
  }

  public void acquire(String id) throws InterruptedException {
    long threadId = Thread.currentThread().getId();
    if (locks.containsKey(threadId)) {
      throw new RuntimeException("Cannot acquire more than one lock per thread at the same time");
    }
    Lock lock = client.getLock(key(id));
    LockContext context = new LockContext(lock, id);
    locks.put(threadId, context);

    try {
      context.acquire(timeout, TimeUnit.MILLISECONDS);
      locksAcquired.incrementAndGet();
      waitedForLocksMs.addAndGet(context.waitedForAcquisition().toMillis());
    } catch (InterruptedException e) {
      locks.remove(threadId);
      throw e;
    }
  }

  public void release() {
    long threadId = Thread.currentThread().getId();
    LockContext context = locks.remove(threadId);
    if (context == null) {
      return;
    }
    context.release();
    locksHeldMs.addAndGet(context.lockHeld().toMillis());
  }

  public String key(String id) {
    return keyspace + "::" + id;
  }

  private static LockManager create(
      String address, String auth, String keyspace, long lockLifetime, Duration timeout) {
    Config config = new Config();
    config.useSingleServer().setAddress(address).setPassword(auth);
    config.setLockWatchdogTimeout(lockLifetime);
    RedissonClient client = Redisson.create(config);
    return new LockManager(client, keyspace, timeout);
  }

  /** @return the total number of successfully acquired locks */
  public AtomicLong getLocksAcquired() {
    return locksAcquired;
  }

  /**
   * @return the total amount of time spent on waiting for locks. Time for locks held at the same
   *     time is added up.
   */
  public AtomicLong getWaitedForLocksMs() {
    return waitedForLocksMs;
  }

  /**
   * @return the total amount of time all locks have been held. Time for locks held at the same time
   *     is added up.
   */
  public AtomicLong getLocksHeldMs() {
    return locksHeldMs;
  }

  public boolean threadHasLock() {
    long threadId = Thread.currentThread().getId();
    return locks.containsKey(threadId);
  }

  public Optional<String> getLockedIdForThread() {
    long threadId = Thread.currentThread().getId();
    var context = locks.get(threadId);
    if (context == null) {
      return Optional.empty();
    }
    return Optional.of(context.getId());
  }
}
