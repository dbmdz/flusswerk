package com.github.dbmdz.flusswerk.framework.config.properties;

import static java.util.Objects.requireNonNullElseGet;

import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/** Model for the configuration parameters in <code>application.yml</code>. */
@ConstructorBinding
@ConfigurationProperties(prefix = "flusswerk")
public class FlusswerkProperties {

  @NestedConfigurationProperty private final Processing processing;

  @NestedConfigurationProperty private final RabbitMQ rabbitMQ;

  @NestedConfigurationProperty private final Routing routing;

  @NestedConfigurationProperty private final Monitoring monitoring;

  @NestedConfigurationProperty private final Redis redis;

  @ConstructorBinding
  public FlusswerkProperties(
      Processing processing,
      RabbitMQ rabbitMQ,
      Routing routing,
      Monitoring monitoring,
      Redis redis) {
    this.processing = requireNonNullElseGet(processing, Processing::defaults);
    this.rabbitMQ = requireNonNullElseGet(rabbitMQ, RabbitMQ::defaults);
    this.routing = requireNonNullElseGet(routing, Routing::defaults);
    this.monitoring = requireNonNullElseGet(monitoring, Monitoring::defaults);
    this.redis = redis; // might actually be null, then centralized locking will be disabled
  }

  public Processing getProcessing() {
    return processing;
  }

  public RabbitMQ getRabbitMQ() {
    return rabbitMQ;
  }

  public Routing getRouting() {
    return routing;
  }

  public Monitoring getMonitoring() {
    return monitoring;
  }

  public Optional<Redis> getRedis() {
    return Optional.ofNullable(redis);
  }

  @Override
  public String toString() {
    return StringRepresentation.of(FlusswerkProperties.class)
        .property("processing", processing.toString())
        .property("routing", routing.toString())
        .property("connection", rabbitMQ.toString())
        .property("monitoring", monitoring.toString())
        .toString();
  }
}
