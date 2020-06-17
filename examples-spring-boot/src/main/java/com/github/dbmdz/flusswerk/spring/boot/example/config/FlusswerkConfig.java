package com.github.dbmdz.flusswerk.spring.boot.example.config;

import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.locking.NoOpLockManager;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.monitoring.BaseMetrics;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import com.github.dbmdz.flusswerk.spring.boot.example.ComposePerfectGreeting;
import com.github.dbmdz.flusswerk.spring.boot.example.FancyConsoleProcessReport;
import com.github.dbmdz.flusswerk.spring.boot.example.model.Greeting;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlusswerkConfig {

  @Bean
  IncomingMessageType incomingMessageType() {
    return new IncomingMessageType(Greeting.class);
  }

  @Bean
  public Flow<Greeting, String, String> flow(BaseMetrics metrics) {
    var flowSpec =
        FlowBuilder.flow(Greeting.class, String.class, String.class)
            .reader(Greeting::getText)
            .transformer(new ComposePerfectGreeting())
            .writerSendingNothing(System.out::println)
            .metrics(metrics)
            .build();
    return new Flow<>(flowSpec, new NoOpLockManager());
  }

  @Bean
  public ProcessReport processReport() {
    return new FancyConsoleProcessReport();
  }
}
