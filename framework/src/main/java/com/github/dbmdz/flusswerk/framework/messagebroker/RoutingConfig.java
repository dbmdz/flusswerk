package com.github.dbmdz.flusswerk.framework.messagebroker;

import com.github.dbmdz.flusswerk.framework.model.Message;

public interface RoutingConfig {

  String getExchange();

  String getDeadLetterExchange();

  String[] getReadFrom();

  String getWriteTo();

  boolean hasWriteTo();

  FailurePolicy getFailurePolicy(String inputQueue);

  default FailurePolicy getFailurePolicy(Message message) {
    return getFailurePolicy(message.getEnvelope().getSource());
  }
}
