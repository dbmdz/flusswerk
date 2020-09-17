package com.github.dbmdz.flusswerk.framework.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The NoOpEngine")
class NoOpEngineTest {

  private NoOpEngine engine;

  @BeforeEach
  void setUp() {
    engine = new NoOpEngine();
  }

  @DisplayName("should never start")
  @Test
  void start() {
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> engine.start());
  }

  @DisplayName("should report stats")
  @Test
  void getStats() {
    EngineStats stats = engine.getStats();
    assertThat(stats.getActiveWorkers()).isZero();
    assertThat(stats.getAvailableWorkers()).isZero();
    assertThat(stats.getConcurrentWorkers()).isZero();
    assertThat(stats.allWorkersBusy()).isTrue();
  }
}
