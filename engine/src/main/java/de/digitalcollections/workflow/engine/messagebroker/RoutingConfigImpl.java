package de.digitalcollections.workflow.engine.messagebroker;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

class RoutingConfigImpl implements RoutingConfig {

  private String exchange;

  private String deadLetterExchange;

  private String[] readFrom;

  private String writeTo;

  private Map<String, FailurePolicy> failurePolicies;

  RoutingConfigImpl() {
    readFrom = new String[] {};
    writeTo = null;
    failurePolicies = new HashMap<>();
  }

  public void complete() {
    if (exchange == null) {
      exchange = "workflow";
    }
    if (deadLetterExchange == null) {
      deadLetterExchange = exchange + ".retry";
    }
    for (String inputQueue : readFrom){
      if (!failurePolicies.containsKey(inputQueue)) {
        failurePolicies.put(inputQueue, new FailurePolicy(inputQueue));
      }
    }
  }

  @Override
  public String getExchange() {
    return exchange;
  }

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }

  @Override
  public String getDeadLetterExchange() {
    return deadLetterExchange;
  }

  public void setDeadLetterExchange(String deadLetterExchange) {
    this.deadLetterExchange = deadLetterExchange;
  }

  @Override
  public String[] getReadFrom() {
    return readFrom;
  }

  public void setReadFrom(String... readFrom) {
    this.readFrom = requireNonNull(readFrom);
  }

  @Override
  public String getWriteTo() {
    return writeTo;
  }

  public void setWriteTo(String writeTo) {
    this.writeTo = writeTo;
  }

  @Override
  public boolean hasWriteTo() {
    return writeTo != null;
  }

  @Override
  public FailurePolicy getFailurePolicy(String inputQueue) {
    return failurePolicies.get(inputQueue);
  }

  public void addFailurePolicy(FailurePolicy failurePolicy) {
    failurePolicies.put(failurePolicy.getInputQueue(), failurePolicy);
  }

}
