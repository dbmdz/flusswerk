package com.github.dbmdz.flusswerk.framework.config.properties;

import static java.util.Objects.requireNonNullElseGet;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/** Model for the configuration parameters in <code>application.yml</code>. */
@ConstructorBinding
@ConfigurationProperties(prefix = "flusswerk")
public class FlusswerkProperties {

  @NestedConfigurationProperty private final Processing processing;

  @NestedConfigurationProperty private final Connection connection;

  @NestedConfigurationProperty private final Routing routing;

  @NestedConfigurationProperty private final Monitoring monitoring;

  @NestedConfigurationProperty private final Redis redis;

  @ConstructorBinding
  public FlusswerkProperties(
      Processing processing,
      Connection connection,
      Routing routing,
      Monitoring monitoring,
      Redis redis) {
    this.processing = processing;
    this.connection = connection;
    this.routing = routing;
    this.monitoring = requireNonNullElseGet(monitoring, () -> new Monitoring("flusswerk"));
    this.redis = redis;
  }

  public Processing getProcessing() {
    return processing;
  }

  public Connection getConnection() {
    return connection;
  }

  public Routing getRouting() {
    return routing;
  }

  public Monitoring getMonitoring() {
    return monitoring;
  }

  public Redis getRedis() {
    return redis;
  }

  @Override
  public String toString() {
    return StringRepresentation.of(FlusswerkProperties.class)
        .property("processing", processing.toString())
        .property("routing", routing.toString())
        .property("connection", connection.toString())
        .property("monitoring", monitoring.toString())
        .toString();
  }
}
