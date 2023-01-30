package com.github.dbmdz.flusswerk.framework.config.properties;

import static java.util.Objects.requireNonNullElseGet;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/** Model for the configuration parameters in <code>application.yml</code>. */
@ConfigurationProperties(prefix = "flusswerk")
public record FlusswerkProperties(
    @NestedConfigurationProperty ProcessingProperties processing,
    @NestedConfigurationProperty RabbitMQProperties rabbitmq,
    @NestedConfigurationProperty RoutingProperties routing,
    @NestedConfigurationProperty MonitoringProperties monitoring) {

  public FlusswerkProperties {
    processing = requireNonNullElseGet(processing, ProcessingProperties::defaults);
    rabbitmq = requireNonNullElseGet(rabbitmq, RabbitMQProperties::defaults);
    routing = requireNonNullElseGet(routing, RoutingProperties::defaults);
    monitoring = requireNonNullElseGet(monitoring, MonitoringProperties::defaults);
  }
}
