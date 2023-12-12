package com.github.dbmdz.flusswerk.framework.engine;

/** A ChannelListener receives notifications about channel recovery. */
public interface ChannelListener {
  void handleReset();
}
