package org.mdz.dzp.workflow.neo.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

public class MessageBrokerBuilder {

  private final MessageBrokerConfig config;

  public MessageBrokerBuilder() {
    config = new MessageBrokerConfig();
    config.setHostName("localhost");
    config.setPassword("guest");
    config.setPort(5672);
    config.setUsername("guest");
    config.setVirtualHost("/");
    config.setObjectMapper(new ObjectMapper());
  }

  public MessageBrokerBuilder hostName(String hostName) {
    config.setHostName(requireNonNull(hostName));
    return this;
  }

  public MessageBrokerBuilder password(String password) {
    config.setPassword(requireNonNull(password));
    return this;
  }

  public MessageBrokerBuilder port(int port) {
    if (port <= 0) {
      throw new IllegalArgumentException("Port value must be > 0");
    }
    config.setPort(port);
    return this;
  }

  public MessageBrokerBuilder username(String username) {
    config.setUsername(requireNonNull(username));
    return this;
  }
  public MessageBrokerBuilder virtualHost(String virtualHost) {
    config.setVirtualHost(requireNonNull(virtualHost));
    return this;
  }

  public MessageBrokerBuilder objectMapper(ObjectMapper objectMapper) {
    config.setObjectMapper(requireNonNull(objectMapper));
    return this;
  }

  public MessageBroker build() throws IOException, TimeoutException {
    return new MessageBroker(config);
  }

}
