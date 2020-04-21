package de.digitalcollections.flusswerk.engine.model;

public abstract class TraceableMessage extends FlusswerkMessage {

  private final String tracingId;

  public TraceableMessage(String tracingId) {
    this.tracingId = tracingId;
  }

  public String getTracingId() {
    return tracingId;
  }
}
