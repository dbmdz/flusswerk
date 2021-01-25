package com.github.dbmdz.flusswerk.framework.engine;

/** Manage data processing for incoming messages. */
public interface Engine {

  /**
   * Start the worker threads and wait for incoming messages to process with {@link
   * com.github.dbmdz.flusswerk.framework.flow.Flow}.
   */
  void start();

  /** Stops all worker threads. */
  void stop();

  /** @return data processing stats for monitoring and debugging. */
  EngineStats getStats();
}
