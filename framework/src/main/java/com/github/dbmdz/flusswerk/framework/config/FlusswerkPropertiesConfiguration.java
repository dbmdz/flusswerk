package com.github.dbmdz.flusswerk.framework.config;

import com.github.dbmdz.flusswerk.framework.config.properties.AppProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.FlusswerkProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** Separate config class for reading configuration properties to enable automated testing. */
@EnableConfigurationProperties({AppProperties.class, FlusswerkProperties.class})
public class FlusswerkPropertiesConfiguration {}
