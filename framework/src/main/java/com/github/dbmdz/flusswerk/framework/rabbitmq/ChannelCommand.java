package com.github.dbmdz.flusswerk.framework.rabbitmq;

import java.io.IOException;

public interface ChannelCommand<T> {
  T execute() throws IOException;
}
