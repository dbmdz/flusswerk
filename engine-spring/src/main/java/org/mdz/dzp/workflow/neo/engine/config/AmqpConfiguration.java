package org.mdz.dzp.workflow.neo.engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;


@Configuration
public class AmqpConfiguration {

  @Bean
  public RetryOperationsInterceptor retryOperationsInterceptor() {
    return RetryInterceptorBuilder.stateless()
        .maxAttempts(1000)
        .build();
  }

}
