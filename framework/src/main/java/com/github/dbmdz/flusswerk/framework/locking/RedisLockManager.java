package com.github.dbmdz.flusswerk.framework.locking;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.redisson.api.RedissonClient;

public class RedisLockManager implements LockManager {

  private final RedissonClient client;

  public RedisLockManager(RedissonClient client) {
    this.client = requireNonNull(client);
  }

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
