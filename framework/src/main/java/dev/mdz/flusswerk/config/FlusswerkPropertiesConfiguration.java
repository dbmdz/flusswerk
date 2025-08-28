package dev.mdz.flusswerk.config;

import dev.mdz.flusswerk.config.properties.AppProperties;
import dev.mdz.flusswerk.config.properties.FlusswerkProperties;
import dev.mdz.flusswerk.config.properties.MonitoringProperties;
import dev.mdz.flusswerk.config.properties.ProcessingProperties;
import dev.mdz.flusswerk.config.properties.RabbitMQProperties;
import dev.mdz.flusswerk.config.properties.RoutingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** Separate config class for reading configuration properties to enable automated testing. */
@EnableConfigurationProperties({
  AppProperties.class,
  FlusswerkProperties.class,
  MonitoringProperties.class,
  ProcessingProperties.class,
  RabbitMQProperties.class,
  RoutingProperties.class
})
public class FlusswerkPropertiesConfiguration {}
