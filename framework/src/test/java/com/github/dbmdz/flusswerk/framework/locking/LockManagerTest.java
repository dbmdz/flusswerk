package com.github.dbmdz.flusswerk.framework.locking;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@DisplayName("The LockManager")
class LockManagerTest {

  @Test
  @DisplayName("should never acquire two locks for the same thread")
  void shouldNeverAcquireTwoLocksForSameThread() throws InterruptedException {
    RedissonClient client = mock(RedissonClient.class);
    RLock lock = mock(RLock.class);
    when(client.getLock(any())).thenReturn(lock);
    LockManager lockManager = new LockManager(client, "", Duration.ofMillis(50));

    lockManager.acquire("1");
    assertThatThrownBy(() -> lockManager.acquire("2"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("more than one lock");
  }
}
