package de.digitalcollections.flusswerk.engine.model;

/**
 * Tagging interface for all {@link Message} implementations that allow the propagation of flow ids
 * from one workflow job to another, enabling Flusswerk to automatically set these.
 */
public interface HasFlowId {

  String getFlowId();

  void setFlowId(String flowId);
}
