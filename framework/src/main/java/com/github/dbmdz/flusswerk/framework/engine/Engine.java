package com.github.dbmdz.flusswerk.framework.engine;

import com.github.dbmdz.flusswerk.framework.model.Message;

/** Manage data processing for incoming messages. */
public interface Engine {

  /**
   * Start the worker threads and wait for incoming messages to process with {@link
   * com.github.dbmdz.flusswerk.framework.flow.Flow}.
   */
  void start();

  /**
   * Executes a {@link com.github.dbmdz.flusswerk.framework.flow.Flow} for {@code message} on a free
   * worker thread. Should block until a free worker thread is available.
   *
   * @param message The message to process with the {@link
   *     com.github.dbmdz.flusswerk.framework.flow.Flow}
   */
  void process(Message message);

  /** Stops all worker threads. */
  void stop();

  /** @return data processing stats for monitoring and debugging. */
  EngineStats getStats();
}
