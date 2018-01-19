package de.digitalcollections.workflow.engine.messagebroker;

import de.digitalcollections.workflow.engine.model.Message;

public interface RoutingConfig {

  String getExchange();

  String getDeadLetterExchange();

  String[] getReadFrom();

  String getWriteTo();

  boolean hasWriteTo();

  FailurePolicy getFailurePolicy(String inputQueue);

  default  FailurePolicy getFailurePolicy(Message message) {
    return getFailurePolicy(message.getEnvelope().getSource());
  }

}
