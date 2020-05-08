package com.github.dbmdz.flusswerk.framework.messagebroker.builder;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.jackson.SingleClassModule;
import com.github.dbmdz.flusswerk.framework.messagebroker.ConnectionConfig;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBrokerConfig;
import com.github.dbmdz.flusswerk.framework.messagebroker.RoutingConfig;
import com.github.dbmdz.flusswerk.framework.model.Message;

public class ViaStep<M extends Message> {

  private final ConnectionConfig connectionConfig;
  private final MessageBrokerConfig<M> messageBrokerConfig;
  private final RoutingConfig routingConfig;

  ViaStep(
      ConnectionConfig connectionConfig,
      MessageBrokerConfig<M> messageBrokerConfig,
      RoutingConfig routingConfig) {
    this.connectionConfig = connectionConfig;
    this.messageBrokerConfig = messageBrokerConfig;
    this.routingConfig = routingConfig;
  }

  public ViaStep<M> usingJacksonMixin(Class<?> mixin) {
    messageBrokerConfig.addJacksonModule(
        new SingleClassModule(messageBrokerConfig.getMessageClass(), requireNonNull(mixin)));
    return this;
  }

  public BuildStep<M> via(RabbitMQ rabbitMQ) {
    connectionConfig.setUsername(rabbitMQ.getUsername());
    connectionConfig.setPassword(rabbitMQ.getPassword());
    connectionConfig.setAddresses(rabbitMQ.getAddresses());
    connectionConfig.setVirtualHost(rabbitMQ.getVirtualHost());
    return new BuildStep<>(connectionConfig, messageBrokerConfig, routingConfig);
  }
}
