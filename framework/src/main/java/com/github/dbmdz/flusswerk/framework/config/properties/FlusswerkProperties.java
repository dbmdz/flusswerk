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

  @NestedConfigurationProperty private final ProcessingProperties processing;

  @NestedConfigurationProperty private final RabbitMQProperties rabbitMQ;

  @NestedConfigurationProperty private final RoutingProperties routing;

  @NestedConfigurationProperty private final MonitoringProperties monitoring;

  @NestedConfigurationProperty private final RedisProperties redis;

  @ConstructorBinding
  public FlusswerkProperties(
      ProcessingProperties processing,
      RabbitMQProperties rabbitMQ,
      RoutingProperties routing,
      MonitoringProperties monitoring,
      RedisProperties redis) {
    this.processing = requireNonNullElseGet(processing, ProcessingProperties::defaults);
    this.rabbitMQ = requireNonNullElseGet(rabbitMQ, RabbitMQProperties::defaults);
    this.routing = requireNonNullElseGet(routing, RoutingProperties::defaults);
    this.monitoring = requireNonNullElseGet(monitoring, MonitoringProperties::defaults);
    this.redis = redis; // might actually be null, then centralized locking will be disabled
  }

  public ProcessingProperties getProcessing() {
    return processing;
  }

  public RabbitMQProperties getRabbitMQ() {
    return rabbitMQ;
  }

  public RoutingProperties getRouting() {
    return routing;
  }

  public MonitoringProperties getMonitoring() {
    return monitoring;
  }

  public Optional<RedisProperties> getRedis() {
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
