package com.github.dbmdz.flusswerk.framework.spring;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** Separate config class for reading configuration properties to enable automated testing. */
@EnableConfigurationProperties(FlusswerkProperties.class)
public class FlusswerkPropertiesConfiguration {}
