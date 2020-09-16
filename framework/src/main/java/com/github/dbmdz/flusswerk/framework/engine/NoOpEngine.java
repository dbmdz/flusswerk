package com.github.dbmdz.flusswerk.framework.engine;

import com.github.dbmdz.flusswerk.framework.model.Message;

/**
 * No operations Engine for Flusswerk applications that use Flusswerk only to send messages, but
 * don't have actual message driven data processing.
 */
public class NoOpEngine implements Engine {

  private static final EngineStats ENGINE_STATS = new EngineStats(0, 0, 0);

  @Override
  public void start() {
    throw new RuntimeException("Cannot start engine. Did you define a Flow bean?");
  }

  @Override
  public void process(Message message) {}

  @Override
  public void stop() {}

  @Override
  public EngineStats getStats() {
    return ENGINE_STATS;
  }
}
