package dev.mdz.flusswerk.rabbitmq;

/** A ChannelListener receives notifications about channel recovery. */
public interface ChannelListener {
  void handleReset();
}
