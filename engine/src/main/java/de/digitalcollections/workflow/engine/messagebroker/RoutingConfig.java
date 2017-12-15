package de.digitalcollections.workflow.engine.messagebroker;

public interface RoutingConfig {

  String getExchange();

  String getDeadLetterExchange();

  String getReadFrom();

  String getWriteTo();

  boolean hasWriteTo();

  String getFailedQueue();

  String getRetryQueue();

  boolean hasFailedQueue();
}
