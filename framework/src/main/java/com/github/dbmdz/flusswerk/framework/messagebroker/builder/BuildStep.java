package com.github.dbmdz.flusswerk.framework.messagebroker.builder;

import com.github.dbmdz.flusswerk.framework.messagebroker.ConnectionConfig;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBrokerConfig;
import com.github.dbmdz.flusswerk.framework.messagebroker.RabbitClient;
import com.github.dbmdz.flusswerk.framework.messagebroker.RabbitConnection;
import com.github.dbmdz.flusswerk.framework.messagebroker.RoutingConfig;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;

public class BuildStep<M extends Message> {

  private final ConnectionConfig connectionConfig;
  private final MessageBrokerConfig<M> messageBrokerConfig;
  private final RoutingConfig routingConfig;

  public BuildStep(
      ConnectionConfig connectionConfig,
      MessageBrokerConfig<M> messageBrokerConfig,
      RoutingConfig routingConfig) {
    this.connectionConfig = connectionConfig;
    this.messageBrokerConfig = messageBrokerConfig;
    this.routingConfig = routingConfig;
  }

  public MessageBroker<M> build() {
    routingConfig.complete();
    try {
      var connection = new RabbitConnection(connectionConfig);
      var client = new RabbitClient<>(messageBrokerConfig, connection);
      return new MessageBroker<>(messageBrokerConfig, routingConfig, client);
    } catch (IOException | RuntimeException e) {
      throw new RuntimeException("Error initializing communications", e);
    }
  }
}
