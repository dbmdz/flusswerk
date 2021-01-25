package com.github.dbmdz.flusswerk.framework.engine;

/**
 * No operations Engine for Flusswerk applications that use Flusswerk only to send messages, but
 * don't have actual message driven data processing.
 */
public class NoOpEngine implements Engine {

  private static final EngineStats ENGINE_STATS = new EngineStats(0, 0, 0);

  /** Start fails with an exception because a no-op engine is supposed to not do any processing. */
  @Override
  public void start() {
    throw new RuntimeException("Cannot start engine. Did you define a Flow bean?");
  }

  /** Does nothing as no processing would ever happen. */
  @Override
  public void stop() {}

  /**
   * Correctly reports the processing of nothing.
   *
   * @return processing information reporting that no processing has happened.
   */
  @Override
  public EngineStats getStats() {
    return ENGINE_STATS;
  }
}
