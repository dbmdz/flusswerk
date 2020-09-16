package com.github.dbmdz.flusswerk.framework.engine;

import com.github.dbmdz.flusswerk.framework.model.Message;

/** Manage data processing for incoming messages. */
public interface Engine {

  void start();

  void process(Message message);

  void stop();

  EngineStats getStats();
}
