package com.github.dbmdz.flusswerk.framework.messagebroker.builder;

import com.github.dbmdz.flusswerk.framework.messagebroker.ConnectionConfig;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBrokerConfig;
import com.github.dbmdz.flusswerk.framework.messagebroker.RoutingConfig;
import com.github.dbmdz.flusswerk.framework.model.Message;

public class MessageBrokerBuilder {

  public static <M extends Message> FromStep<M> read(Class<M> message) {
    var connectionConfig = new ConnectionConfig();
    var messageBrokerConfig = new MessageBrokerConfig<>(message);
    var routingConfig = new RoutingConfig();
    return new FromStep<>(connectionConfig, messageBrokerConfig, routingConfig);
  }

  public static ViaStep<Message> sendTo(String topic) {
    var connectionConfig = new ConnectionConfig();
    var messageBrokerConfig = new MessageBrokerConfig<>(Message.class);
    var routingConfig = new RoutingConfig();
    routingConfig.setWriteTo(topic);
    return new ViaStep<>(connectionConfig, messageBrokerConfig, routingConfig);
  }
}
