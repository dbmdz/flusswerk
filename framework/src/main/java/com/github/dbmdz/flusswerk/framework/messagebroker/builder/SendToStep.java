package com.github.dbmdz.flusswerk.framework.messagebroker.builder;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.jackson.SingleClassModule;
import com.github.dbmdz.flusswerk.framework.messagebroker.ConnectionConfig;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBrokerConfig;
import com.github.dbmdz.flusswerk.framework.messagebroker.RoutingConfig;
import com.github.dbmdz.flusswerk.framework.model.Message;

public class SendToStep<M extends Message> {

  private final ConnectionConfig connectionConfig;
  private final MessageBrokerConfig<M> messageBrokerConfig;
  private final RoutingConfig routingConfig;

  public SendToStep(
      ConnectionConfig connectionConfig,
      MessageBrokerConfig<M> messageBrokerConfig,
      RoutingConfig routingConfig) {
    this.connectionConfig = connectionConfig;
    this.messageBrokerConfig = messageBrokerConfig;
    this.routingConfig = routingConfig;
  }

  public SendToStep<M> usingJacksonMixin(Class<?> mixin) {
    messageBrokerConfig.addJacksonModule(
        new SingleClassModule(messageBrokerConfig.getMessageClass(), requireNonNull(mixin)));
    return this;
  }

  public SendToStep<M> maxRetries(int times) {
    if (times < 0) {
      throw new IllegalArgumentException(
          "Max retries has to be a positive number but was " + times);
    }
    messageBrokerConfig.setMaxRetries(times);
    return this;
  }

  public SendToStep<M> waitBetweenRetries(int seconds) {
    if (seconds < 0) {
      throw new IllegalArgumentException("Time between retries has to be a positive number but was "
          + seconds);
    }
    messageBrokerConfig.setDeadLetterWait(seconds * 1000);
    return this;
  }

  public ViaStep<M> sendTo(String topic) {
    routingConfig.setWriteTo(topic);
    return new ViaStep<>(connectionConfig, messageBrokerConfig, routingConfig);
  }

}
