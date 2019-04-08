package de.digitalcollections.flusswerk.engine.model;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link Message} implementation with an additional identifier to trace jobs through multiple workflow steps.
 */
public class FlowMessage extends DefaultMessage implements HasFlowId {

  private long flowId;

  protected FlowMessage() {
  }

  public FlowMessage(String id) {
    super(id);
    this.flowId = -1;
  }

  public FlowMessage(String id, long flowId) {
    super(id);
    this.flowId = flowId;
  }

  public long getFlowId() {
    return flowId;
  }

  @Override
  public void setFlowId(long flowId) {
    this.flowId = flowId;
  }

  @Override
  public String toString() {
    return "Message{id=" + getId() + ", flowId=" + getFlowId()
           + ", envelope=" + getEnvelope() + ", data=" + getData() + "}";
  }

}
