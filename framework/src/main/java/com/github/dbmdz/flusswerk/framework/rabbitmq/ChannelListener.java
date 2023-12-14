package com.github.dbmdz.flusswerk.framework.rabbitmq;

/** A ChannelListener receives notifications about channel recovery. */
public interface ChannelListener {
  void handleReset();
}
