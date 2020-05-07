package com.github.dbmdz.flusswerk.framework.messagebroker;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.Module;
import com.github.dbmdz.flusswerk.framework.jackson.DefaultMixin;
import com.github.dbmdz.flusswerk.framework.jackson.SingleClassModule;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;

/**
 * Builder to create an instance of the {@link MessageBroker} which manages the connection to
 * RabbitMQ and all related configuration and setups like creating queues and exchanges.
 */
public class MessageBrokerBuilder {

  private final ConnectionConfigImpl connectionConfig;

  private final MessageBrokerConfigImpl config;

  private final RoutingConfigImpl routingConfig;

  public MessageBrokerBuilder() {
    config = new MessageBrokerConfigImpl();
    connectionConfig = new ConnectionConfigImpl();
    routingConfig = new RoutingConfigImpl();
  }

  /**
   * Adds an RabbitMQ host to connect to. Default would be <em>localhost</em> with port 5672. To
   * configure cluster access call this method once for each server.
   *
   * @param host The host to connect to.
   * @param port The port to use for this connection
   * @return This {@link MessageBrokerBuilder} instance to chain configuration calls.
   */
  public MessageBrokerBuilder connectTo(String host, int port) {
    connectionConfig.addAddress(host, port);
    return this;
  }

  public MessageBrokerBuilder connectTo(String connectionStr) {
    if (connectionStr != null) {
      String[] connections = connectionStr.split(",|;");
      for (String connection : connections) {
        String[] connectionParts = connection.split(":");
        if (connectionParts.length != 2) {
          throw new RuntimeException(
              "Invalid connection specified: '" + connection + "'. Must be of format host:port");
        }
        connectionConfig.addAddress(connectionParts[0], Integer.parseInt(connectionParts[1]));
      }
    }
    return this;
  }

  /**
   * Sets the RabbitMQ password for authentication.
   *
   * @param password The password for authentication.
   * @return This {@link MessageBrokerBuilder} instance to chain configuration calls.
   */
  public MessageBrokerBuilder password(String password) {
    connectionConfig.setPassword(requireNonNull(password));
    return this;
  }

  /**
   * Sets the RabbitMQ username for authentication.
   *
   * @param username The username for authentication.
   * @return This {@link MessageBrokerBuilder} instance to chain configuration calls.
   */
  public MessageBrokerBuilder username(String username) {
    connectionConfig.setUsername(requireNonNull(username));
    return this;
  }

  /**
   * Sets the internal RabbitMQ virtualHost (default is "\").
   *
   * @param virtualHost The virtual host.
   * @return This {@link MessageBrokerBuilder} instance to chain configuration calls.
   */
  public MessageBrokerBuilder virtualHost(String virtualHost) {
    connectionConfig.setVirtualHost(requireNonNull(virtualHost));
    return this;
  }

  /**
   * Registers Jackson Modules to use with the object mapper.
   *
   * @param modules The Jackson @{@link Module}s to register
   * @return This {@link MessageBrokerBuilder} instance to chain configuration calls.
   */
  public MessageBrokerBuilder jacksonModules(Module... modules) {
    for (Module module : modules) {
      config.addJacksonModule(module);
    }
    return this;
  }

  /**
   * Sets the time to wait for dead-lettered messages before these are returned to the queue.
   *
   * @param milliseconds The waiting time.
   * @return This {@link MessageBrokerBuilder} instance to chain configuration calls.
   */
  public MessageBrokerBuilder deadLetterWait(int milliseconds) {
    config.setDeadLetterWait(milliseconds);
    return this;
  }

  /**
   * Sets the maximum number of attempts before a message is sent to the failed queue instead of the
   * dead letter queue.
   *
   * @param number The maximum number of attempts to process a message.
   * @return This {@link MessageBrokerBuilder} instance to chain configuration calls.
   */
  public MessageBrokerBuilder maxRetries(int number) {
    if (number < 0) {
      throw new IllegalArgumentException("Max number of retries must be at least 0.");
    }
    config.setMaxRetries(number);
    return this;
  }

  /**
   * Registers a custom {@link Message} implementation.
   *
   * @param cls The custom message implementation you want to use.
   * @return This {@link MessageBrokerBuilder} instance to chain configuration calls.
   */
  public MessageBrokerBuilder useMessageClass(Class<? extends Message> cls) {
    return useMessageClass(cls, DefaultMixin.class);
  }

  /**
   * Registers a custom {@link Message} implementation) and custom Jackson mixin for more fine
   * grained serialization control.
   *
   * @param cls The custom message implementation you want to use.
   * @param mixin The Jackson mixin to use
   * @return This {@link MessageBrokerBuilder} instance to chain configuration calls.
   */
  public MessageBrokerBuilder useMessageClass(Class<? extends Message> cls, Class<?> mixin) {
    config.addJacksonModule(new SingleClassModule(cls, mixin));
    config.setMessageClass(cls);
    return this;
  }

  /**
   * Sets the AMQP exchange (optional, defaults to 'workflow'). If an exchange does not exist it
   * will be created.
   *
   * @param exchange The regular exchange.
   * @return This {@link MessageBrokerBuilder} instance to chain configuration calls.
   */
  public MessageBrokerBuilder exchange(String exchange) {
    routingConfig.setExchange(requireNonNull(exchange));
    return this;
  }

  /**
   * Sets the AMQP dead letter exchange (optional, defaults to 'workflow.dlx'). If an exchange does
   * not exist it will be created.
   *
   * @param deadLetterExchange The dead letter exchange.
   * @return This {@link MessageBrokerBuilder} instance to chain configuration calls.
   */
  public MessageBrokerBuilder deadLetterExchange(String deadLetterExchange) {
    if (deadLetterExchange == null || deadLetterExchange.isEmpty()) {
      throw new IllegalArgumentException("Dead letter exchange must not be null or empty");
    }
    routingConfig.setDeadLetterExchange(deadLetterExchange);
    return this;
  }

  /**
   * Finally builds the {@link MessageBroker} as configured, up and running and connected it to
   * RabbitMQ.
   *
   * @return A new MessageBroker
   * @throws RuntimeException If communication with RabbitMQ fails.
   */
  public MessageBroker build() {
    routingConfig.complete();
    try {
      return build(new RabbitConnection(connectionConfig));
    } catch (IOException | RuntimeException e) {
      throw new RuntimeException("Error initializing communications", e);
    }
  }

  MessageBroker build(RabbitConnection connection) throws IOException {
    routingConfig.complete();
    return new MessageBroker(config, routingConfig, new RabbitClient(config, connection));
  }

  /**
   * The queues to read from. All queues are considered in order. Only if a queue is empty, the next
   * queue will be queried.
   *
   * @param inputQueues The queue names to read from.
   * @return This builder for a fluent interface.
   */
  public MessageBrokerBuilder readFrom(String... inputQueues) {
    if (inputQueues == null || inputQueues.length == 0) {
      throw new IllegalArgumentException("The input queue cannot be null or empty.");
    }
    routingConfig.setReadFrom(inputQueues);
    return this;
  }

  public MessageBrokerBuilder addFailurePolicy(FailurePolicy failurePolicy) {
    if (failurePolicy == null) {
      throw new IllegalArgumentException("A failure policy cannot be null");
    }
    routingConfig.addFailurePolicy(failurePolicy);
    return this;
  }

  public MessageBrokerBuilder writeTo(String outputRoutingKey) {
    routingConfig.setWriteTo(outputRoutingKey);
    return this;
  }

  ConnectionConfig getConnectionConfig() {
    return connectionConfig;
  }
}
