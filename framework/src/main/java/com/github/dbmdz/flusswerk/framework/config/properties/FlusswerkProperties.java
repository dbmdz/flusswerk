package com.github.dbmdz.flusswerk.framework.config.properties;

import static java.util.Objects.requireNonNullElseGet;

import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/** Model for the configuration parameters in <code>application.yml</code>. */
@ConstructorBinding
@ConfigurationProperties(prefix = "flusswerk")
public class FlusswerkProperties {

  @NestedConfigurationProperty private final ProcessingProperties processing;

  @NestedConfigurationProperty private final RabbitMQProperties rabbitmq;

  @NestedConfigurationProperty private final RoutingProperties routing;

  @NestedConfigurationProperty private final MonitoringProperties monitoring;

  @NestedConfigurationProperty private final RedisProperties redis;

  private final Yaml yaml;

  @ConstructorBinding
  public FlusswerkProperties(
      ProcessingProperties processing,
      RabbitMQProperties rabbitmq,
      RoutingProperties routing,
      MonitoringProperties monitoring,
      RedisProperties redis) {
    this.processing = requireNonNullElseGet(processing, ProcessingProperties::defaults);
    this.rabbitmq = requireNonNullElseGet(rabbitmq, RabbitMQProperties::defaults);
    this.routing = requireNonNullElseGet(routing, RoutingProperties::defaults);
    this.monitoring = requireNonNullElseGet(monitoring, MonitoringProperties::defaults);
    this.redis = redis; // might actually be null, then centralized locking will be disabled
    DumperOptions options = new DumperOptions();
    options.setAllowReadOnlyProperties(true);
    yaml = new Yaml(options);
  }

  public ProcessingProperties getProcessing() {
    return processing;
  }

  public RabbitMQProperties getRabbitMQ() {
    return rabbitmq;
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
    return StringRepresentation.of(this);
  }
}
