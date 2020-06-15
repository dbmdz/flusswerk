package com.github.dbmdz.flusswerk.framework.config.properties;

import static java.util.Objects.requireNonNullElseGet;

import com.github.dbmdz.flusswerk.framework.messagebroker.FailurePolicy;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotBlank;

/** AMQP/RabbitMQ routing information. */
public class Routing {

  @NotBlank private final String exchange;
  private final String deadLetterExchange;
  private final List<String> readFrom;
  private final String writeTo;
  private final Map<String, FailurePolicy> failurePolicies;

  /**
   * @param exchange The exchange name to use (required).
   * @param readFrom The queue to read from (optional).
   * @param writeTo The topic to send to per default (optional).
   */

  public Routing(
      @NotBlank String exchange,
      List<String> readFrom,
      String writeTo,
      Map<String, FailurePolicyProperties> failurePolicies) {
    this.exchange = exchange;
    this.deadLetterExchange = exchange + ".dlx";
    this.readFrom = requireNonNullElseGet(readFrom, Collections::emptyList);
    this.writeTo = writeTo;

    this.failurePolicies = createFailurePolicies(readFrom, requireNonNullElseGet(failurePolicies, Collections::emptyMap));
  }

  private static Map<String, FailurePolicy> createFailurePolicies(
      List<String> readFrom, Map<String, FailurePolicyProperties> failurePolicies) {
    var result = new HashMap<String, FailurePolicy>();
    for (String input : failurePolicies.keySet()) {
      var spec = failurePolicies.get(input);
      var failurePolicy =
          new FailurePolicy(
              input, spec.getRetryRoutingKey(), spec.getFailedRoutingKey(), spec.getRetries(), spec.getDeadLetterWait());
      result.put(input, failurePolicy);
    }
    for (String input : readFrom) {
      if (result.containsKey(input)) {
        continue;
      }
      result.put(input, new FailurePolicy(input));
    }
    return result;
  }

  /** @return The exchange name to use (required). */
  public String getExchange() {
    return exchange;
  }

  /** @return The queue to read from (optional). */
  public List<String> getReadFrom() {
    return readFrom;
  }

  /** @return The topic to send to per default (optional). */
  public Optional<String> getWriteTo() {
    return Optional.ofNullable(writeTo);
  }

  public FailurePolicy getFailurePolicy(String queue) {
    return failurePolicies.get(queue);
  }

  public String getDeadLetterExchange() {
    return deadLetterExchange;
  }

  @Override
  public String toString() {
    return StringRepresentation.of(Routing.class)
        .property("exchange", exchange)
        .property("readFrom", String.join(",", readFrom))
        .property("writeTo", writeTo)
        .toString();
  }

  public FailurePolicy getFailurePolicy(Message message) {
    return getFailurePolicy(message.getEnvelope().getSource());
  }

  public static class FailurePolicyProperties {

    private final Integer retries;
    private final String retryRoutingKey;
    private final String failedRoutingKey;
    private final Integer deadLetterWait;

    public FailurePolicyProperties(
        Integer retries, String retryRoutingKey, String failedRoutingKey, Integer deadLetterWait) {
      this.retries = retries;
      this.retryRoutingKey = retryRoutingKey;
      this.failedRoutingKey = failedRoutingKey;
      this.deadLetterWait = deadLetterWait;
    }

    public int getRetries() {
      return retries;
    }

    public String getRetryRoutingKey() {
      return retryRoutingKey;
    }

    public String getFailedRoutingKey() {
      return failedRoutingKey;
    }

    public Integer getDeadLetterWait() {
      return deadLetterWait;
    }
  }
}
