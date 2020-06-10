package com.github.dbmdz.flusswerk.framework.spring;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot annotation to enable automatic configuration of Flusswerk via <code>application.yml
 * </code> and provide beans for autowiring.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import(FlusswerkConfiguration.class)
@Configuration
public @interface EnableFlusswerk {}
