package com.github.dbmdz.flusswerk.framework.locking;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import org.redisson.api.RedissonClient;

public class RedisLockManager implements LockManager {

  private final RedissonClient client;
  private final ConcurrentMap<Long, LockContext> locks;
  private final String keyspace;
  private final long timeout;
  private final AtomicLong locksAcquired;
  private final AtomicLong waitedForLocksNs;
  private final AtomicLong locksHeldNs;
  private final Watch watch;

  public RedisLockManager(RedissonClient client, String keyspace, Duration timeout) {
    this(client, keyspace, timeout, new SystemWatch());
  }

  public RedisLockManager(RedissonClient client, String keyspace, Duration timeout, Watch watch) {
    this.client = requireNonNull(client);
    this.keyspace = requireNonNull(keyspace);
    this.timeout = timeout.toMillis();
    this.locks = new ConcurrentHashMap<>();
    locksAcquired = new AtomicLong();
    waitedForLocksNs = new AtomicLong();
    locksHeldNs = new AtomicLong();
    this.watch = watch;
  }

  @Override
  public void acquire(String id) throws InterruptedException {
    acquire(id, Thread.currentThread().getId());
  }

  void acquire(String id, long threadId) throws InterruptedException {
    if (locks.containsKey(threadId)) {
      throw new RuntimeException("Cannot acquire more than one lock per thread at the same time");
    }
    Lock lock = client.getLock(key(id));
    LockContext context = new LockContext(lock, id, watch);
    locks.put(threadId, context);

    try {
      context.acquire(timeout, TimeUnit.MILLISECONDS);
      locksAcquired.incrementAndGet();
      waitedForLocksNs.addAndGet(context.waitedForAcquisitionNs());
    } catch (InterruptedException e) {
      locks.remove(threadId);
      throw e;
    }
  }

  String key(String id) {
    return keyspace + "::" + id;
  }

  @Override
  public void release() {
    release(Thread.currentThread().getId());
  }

  public void release(long threadId) {
    LockContext context = locks.remove(threadId);
    if (context == null) {
      return;
    }
    context.release();
    locksHeldNs.addAndGet(context.lockHeldNs());
  }

  @Override
  public long getLocksAcquired() {
    return locksAcquired.get();
  }

  @Override
  public long getWaitedForLocksNs() {
    return waitedForLocksNs.get();
  }

  @Override
  public long getLocksHeldNs() {
    return locksHeldNs.get();
  }

  @Override
  public boolean threadHasLock() {
    long threadId = Thread.currentThread().getId();
    return locks.containsKey(threadId);
  }

  @Override
  public Optional<String> getLockedIdForThread() {
    long threadId = Thread.currentThread().getId();
    var context = locks.get(threadId);
    if (context == null) {
      return Optional.empty();
    }
    return Optional.of(context.getId());
  }
}
