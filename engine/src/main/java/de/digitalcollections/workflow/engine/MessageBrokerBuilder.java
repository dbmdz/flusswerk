package de.digitalcollections.workflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import de.digitalcollections.workflow.engine.model.Message;

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
    config.setMaxRetries(5);
    config.setDeadLetterWait(30 * 1000);
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

  public MessageBrokerBuilder deadLetterWait(int milliseconds) {
    config.setDeadLetterWait(milliseconds);
    return this;
  }

  public MessageBrokerBuilder maxRetries(int number) {
    if (number < 0) {
      throw new IllegalArgumentException("Max number of retries must be at least 0.");
    }
    config.setMaxRetries(number);
    return this;
  }

  public MessageBrokerBuilder messageMapping(Class<? extends Message> messageClass, Class<?> messageMixin) {
    config.setMessageClass(messageClass);
    config.setMessageMixin(messageMixin);
    return this;
  }

  public MessageBrokerBuilder exchanges(String exchange, String deadLetterExchange) {
    config.setExchange(requireNonNull(exchange));
    config.setDeadLetterExchange(requireNonNull(deadLetterExchange));
    return this;
  }

  public MessageBroker build() throws IOException, TimeoutException {
    MessageBrokerConnection connection = new MessageBrokerConnection(config);
    return new MessageBroker(config, connection);
  }
}
