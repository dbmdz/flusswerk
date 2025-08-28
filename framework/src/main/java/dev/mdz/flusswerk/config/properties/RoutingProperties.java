package dev.mdz.flusswerk.config.properties;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

import dev.mdz.flusswerk.model.Message;
import dev.mdz.flusswerk.rabbitmq.FailurePolicy;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** AMQP/RabbitMQ routing information. */
@ConfigurationProperties(prefix = "flusswerk.routing")
public class RoutingProperties {

  public static final String DEFAULT_EXCHANGE = "flusswerk_default";

  private final String defaultExchange;
  private final String deadLetterExchange;
  private final List<String> incoming;
  private final Map<String, String> exchanges;
  private final Map<String, String> deadLetterExchanges;
  private final Map<String, List<String>> outgoing;
  private final Map<String, FailurePolicy> failurePolicies;

  /**
   * @param exchange The exchange name to use (required).
   * @param incoming The queue to read from (optional).
   * @param outgoing The topic to send to per default (optional).
   */
  public RoutingProperties(
      @NotBlank String exchange,
      List<String> incoming,
      Map<String, List<String>> outgoing,
      Map<String, String> exchanges,
      Map<String, String> deadLetterExchanges,
      Map<String, FailurePolicyProperties> failurePolicies) {
    this.defaultExchange = requireNonNullElse(exchange, "flusswerk_default");
    this.deadLetterExchange = defaultDeadLetterExchange(this.defaultExchange);
    this.incoming = requireNonNullElseGet(incoming, Collections::emptyList);
    this.outgoing = requireNonNullElseGet(outgoing, Collections::emptyMap); // might be null

    this.exchanges = new HashMap<>();
    this.deadLetterExchanges = new HashMap<>();
    setupExchangeConfigurations(
        this.defaultExchange,
        this.deadLetterExchange,
        requireNonNullElse(exchanges, emptyMap()), // user input, not class attribute
        requireNonNullElse(deadLetterExchanges, emptyMap())); // user input, not class attribute

    failurePolicies = requireNonNullElseGet(failurePolicies, Collections::emptyMap);
    for (String queue : failurePolicies.keySet()) {
      if (!this.incoming.contains(queue)) {
        throw new IllegalArgumentException(
            String.format("FailurePolicy for queue '%s' does not match any incoming queue", queue));
      }
    }

    this.failurePolicies =
        createFailurePolicies(
            this.incoming, requireNonNullElseGet(failurePolicies, Collections::emptyMap));
  }

  /**
   * Create RoutingProperties by only specifying input queues and routes to support simple tests.
   *
   * @param incoming The incoming queues.
   * @param outgoing The routes for outgoing messages.
   * @return routing properties that rely on defaults wherever possible
   */
  public static RoutingProperties minimal(
      List<String> incoming, Map<String, List<String>> outgoing) {
    return new RoutingProperties(null, incoming, outgoing, null, null, null);
  }

  private void setupExchangeConfigurations(
      String defaultExchange,
      String defaultDlx,
      Map<String, String> specificExchanges,
      Map<String, String> specificDeadLetterExchanges) {
    Stream.concat(this.incoming.stream(), this.outgoing.values().stream().flatMap(List::stream))
        .forEach(
            queue -> {
              String exchange = specificExchanges.getOrDefault(queue, defaultExchange);
              this.exchanges.put(queue, exchange);
              String deadLetterExchange =
                  specificDeadLetterExchanges.getOrDefault(queue, defaultDlx);
              this.deadLetterExchanges.put(queue, deadLetterExchange);
            });
  }

  private static Map<String, FailurePolicy> createFailurePolicies(
      List<String> readFrom, Map<String, FailurePolicyProperties> failurePolicies) {
    var result = new HashMap<String, FailurePolicy>();
    for (String input : failurePolicies.keySet()) {
      var spec = failurePolicies.get(input);
      var failurePolicy =
          new FailurePolicy(
              input,
              spec.getRetryRoutingKey(),
              spec.getFailedRoutingKey(),
              spec.getRetries(),
              spec.getBackoff());
      result.put(input, failurePolicy);
    }
    for (String input : readFrom) {
      if (result.containsKey(input)) { // don't overwrite user-defined failure policies
        continue;
      }
      result.put(input, new FailurePolicy(input));
    }
    return result;
  }

  /**
   * @return The exchange name to use (required).
   */
  @Deprecated
  public String getDefaultExchange() {
    return defaultExchange;
  }

  /**
   * @return The exchange name to use (required).
   */
  public String getExchange(String queue) {
    return exchanges.getOrDefault(queue, defaultExchange);
  }

  /**
   * @return The queue to read from (optional).
   */
  public List<String> getIncoming() {
    return incoming;
  }

  /**
   * @return The queues to send to, organized by route (optional).
   */
  public Map<String, List<String>> getOutgoing() {
    return outgoing;
  }

  /**
   * @return A list of all the queues to send to.
   */
  public List<String> allOutgoing() {
    return outgoing.values().stream().flatMap(List::stream).toList();
  }

  public FailurePolicy getFailurePolicy(String queue) {
    return failurePolicies.get(queue);
  }

  @Deprecated
  public String getDeadLetterExchange() {
    return deadLetterExchange;
  }

  public String getDeadLetterExchange(String queue) {
    return deadLetterExchanges.get(queue);
  }

  public FailurePolicy getFailurePolicy(Message message) {
    return getFailurePolicy(message.getEnvelope().getSource());
  }

  public Set<String> getExchanges() {
    return new HashSet<>(exchanges.values());
  }

  public Set<String> getDeadLetterExchanges() {
    return new HashSet<>(deadLetterExchanges.values());
  }

  public static RoutingProperties defaults() {
    return new RoutingProperties(null, null, null, null, null, null);
  }

  public static class FailurePolicyProperties {

    private final Integer retries;
    private final String retryRoutingKey;
    private final String failedRoutingKey;
    private final Duration backoff;

    public FailurePolicyProperties(
        Integer retries, String retryRoutingKey, String failedRoutingKey, Duration backoff) {
      this.retries = retries;
      this.retryRoutingKey = retryRoutingKey;
      this.failedRoutingKey = failedRoutingKey;
      this.backoff = backoff;
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

    public Duration getBackoff() {
      return backoff;
    }
  }

  public static String defaultDeadLetterExchange(String exchange) {
    return exchange + ".retry";
  }
}
