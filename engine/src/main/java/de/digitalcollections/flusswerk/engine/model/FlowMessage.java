package de.digitalcollections.flusswerk.engine.model;

/**
 * A {@link Message} implementation with an additional identifier to trace jobs through multiple
 * workflow steps.
 */
public class FlowMessage extends DefaultMessage implements HasFlowId {

  private String flowId;

  protected FlowMessage() {}

  /**
   * Creates an instance without <code>flowId</code>, in case the Flusswerk shall handle <code>
   * flowId</code> propagation.
   *
   * @param id The id of the processed object
   */
  public FlowMessage(String id) {
    super(id);
    this.flowId = null;
  }

  /**
   * Creates an instance with <code>flowId</code> for manual handling of <code>flowIds</code>.
   *
   * @param id The id of the processed object
   * @param flowId The tracing id through the workflow
   */
  public FlowMessage(String id, String flowId) {
    super(id);
    this.flowId = flowId;
  }

  public String getFlowId() {
    return flowId;
  }

  @Override
  public void setFlowId(String flowId) {
    this.flowId = flowId;
  }

  @Override
  public String toString() {
    return "Message{id="
        + getId()
        + ", flowId="
        + getFlowId()
        + ", envelope="
        + getEnvelope()
        + ", data="
        + getData()
        + "}";
  }
}
