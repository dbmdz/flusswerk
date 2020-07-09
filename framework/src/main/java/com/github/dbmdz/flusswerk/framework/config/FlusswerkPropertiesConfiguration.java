package com.github.dbmdz.flusswerk.framework.config;

import com.github.dbmdz.flusswerk.framework.config.properties.AppProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.FlusswerkProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.MonitoringProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.ProcessingProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RabbitMQProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RedisProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** Separate config class for reading configuration properties to enable automated testing. */
@EnableConfigurationProperties({
  AppProperties.class,
  FlusswerkProperties.class,
  MonitoringProperties.class,
  ProcessingProperties.class,
  RabbitMQProperties.class,
  RedisProperties.class,
  RoutingProperties.class
})
public class FlusswerkPropertiesConfiguration {}
