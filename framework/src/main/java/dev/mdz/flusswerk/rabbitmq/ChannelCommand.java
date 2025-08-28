package dev.mdz.flusswerk.rabbitmq;

import java.io.IOException;

public interface ChannelCommand<T> {
  T execute() throws IOException;
}
