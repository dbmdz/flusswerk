package com.github.dbmdz.flusswerk.framework.messagebroker;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.HashMap;
import java.util.Map;

public class RoutingConfig {

  private String exchange;

  private String deadLetterExchange;

  private String[] readFrom;

  private String writeTo;

  private final Map<String, FailurePolicy> failurePolicies;

  public RoutingConfig() {
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
    for (String inputQueue : readFrom) {
      if (!failurePolicies.containsKey(inputQueue)) {
        failurePolicies.put(inputQueue, new FailurePolicy(inputQueue));
      }
    }
  }

  public String getExchange() {
    return exchange;
  }

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }

  public String getDeadLetterExchange() {
    return deadLetterExchange;
  }

  public void setDeadLetterExchange(String deadLetterExchange) {
    this.deadLetterExchange = deadLetterExchange;
  }

  public String[] getReadFrom() {
    return readFrom;
  }

  public void setReadFrom(String... readFrom) {
    this.readFrom = requireNonNull(readFrom);
  }

  public String getWriteTo() {
    return writeTo;
  }

  public void setWriteTo(String writeTo) {
    this.writeTo = writeTo;
  }

  public boolean hasWriteTo() {
    return writeTo != null;
  }

  public FailurePolicy getFailurePolicy(String inputQueue) {
    return failurePolicies.get(inputQueue);
  }

  public FailurePolicy getFailurePolicy(Message message) {
    return getFailurePolicy(message.getEnvelope().getSource());
  }

  public void addFailurePolicy(FailurePolicy failurePolicy) {
    failurePolicies.put(failurePolicy.getInputQueue(), failurePolicy);
  }
}
