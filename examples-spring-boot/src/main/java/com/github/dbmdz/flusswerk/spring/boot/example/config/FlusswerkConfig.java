package com.github.dbmdz.flusswerk.spring.boot.example.config;

import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.flow.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import com.github.dbmdz.flusswerk.spring.boot.example.ComposePerfectGreeting;
import com.github.dbmdz.flusswerk.spring.boot.example.FancyConsoleProcessReport;
import com.github.dbmdz.flusswerk.spring.boot.example.model.Greeting;
import com.github.dbmdz.flusswerk.spring.boot.starter.MessageImplementation;
import com.github.dbmdz.flusswerk.spring.boot.starter.monitoring.BaseMetrics;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlusswerkConfig {

  @Bean
  MessageImplementation messageImplementation() {
    return new MessageImplementation(Greeting.class);
  }

  @Bean
  public Flow<Greeting, String, String> flow(BaseMetrics metrics) {
    return new FlowBuilder<Greeting, String, String>()
        .read(Greeting::getText)
        .transform(new ComposePerfectGreeting())
        .write((Consumer<String>) System.out::println)
        .measure(metrics)
        .build();
  }

  @Bean
  public ProcessReport processReport() {
    return new FancyConsoleProcessReport();
  }
}
