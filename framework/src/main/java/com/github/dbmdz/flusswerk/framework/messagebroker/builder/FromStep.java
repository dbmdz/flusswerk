package com.github.dbmdz.flusswerk.framework.messagebroker.builder;

import com.github.dbmdz.flusswerk.framework.messagebroker.ConnectionConfig;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBrokerConfig;
import com.github.dbmdz.flusswerk.framework.messagebroker.RoutingConfig;
import com.github.dbmdz.flusswerk.framework.model.Message;

public class FromStep<M extends Message> {

  private final ConnectionConfig connectionConfig;
  private final MessageBrokerConfig<M> messageBrokerConfig;
  private final RoutingConfig routingConfig;

  public FromStep(
      ConnectionConfig connectionConfig,
      MessageBrokerConfig<M> messageBrokerConfig,
      RoutingConfig routingConfig) {
    this.connectionConfig = connectionConfig;
    this.messageBrokerConfig = messageBrokerConfig;
    this.routingConfig = routingConfig;
  }

  public SendToStep<M> from(String... queues) {
    routingConfig.setReadFrom(queues);
    return new SendToStep<>(connectionConfig, messageBrokerConfig, routingConfig);
  }
}
