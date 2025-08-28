package dev.mdz.flusswerk.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The TestingExecutorService")
class TestingExecutorServiceTest {

  private TestingExecutorService executorService;

  @BeforeEach
  void setUp() {
    executorService = new TestingExecutorService();
  }

  @DisplayName("should shutdown")
  @Test
  void shutdown() {
    assertThat(executorService.isShutdown()).isFalse();
    executorService.shutdown();
    assertThat(executorService.isShutdown()).isTrue();
  }

  @DisplayName("should shutdown now")
  @Test
  void shutdownNow() {
    executorService.shutdownNow();
    assertThat(executorService.isShutdown()).isTrue();
  }

  @DisplayName("should terminate with await termination")
  @Test
  void awaitTermination() {
    executorService.shutdown();
    assertThat(executorService.awaitTermination(0, TimeUnit.SECONDS)).isTrue();
    assertThat(executorService.isTerminated()).isTrue();
  }

  @DisplayName("calls Runnable.run() when executing")
  @Test
  void execute() {
    Runnable runnable = mock(Runnable.class);
    executorService.execute(runnable);
    verify(runnable).run();
  }

  @DisplayName("should block new tasks when shutdown")
  @Test
  void shutdownBlocksNewTasks() {
    executorService.shutdown();
    assertThrows(RuntimeException.class, () -> executorService.execute(mock(Runnable.class)));
  }
}
