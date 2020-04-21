package com.github.dbmdz.flusswerk.spring.boot.example.config;

import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.flow.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.model.DefaultMessage;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import com.github.dbmdz.flusswerk.spring.boot.example.ComposePerfectGreeting;
import com.github.dbmdz.flusswerk.spring.boot.example.FancyConsoleProcessReport;
import com.github.dbmdz.flusswerk.spring.boot.example.Metrics;
import com.github.dbmdz.flusswerk.spring.boot.example.model.Greeting;
import com.github.dbmdz.flusswerk.spring.boot.starter.MessageImplementation;
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
  public Flow<DefaultMessage, String, String> flow(Metrics metrics) {
    return new FlowBuilder<DefaultMessage, String, String>()
        .read(message -> message.get("name"))
        .transform(new ComposePerfectGreeting())
        .write((Consumer<String>) System.out::println)
        .monitor(metrics)
        .build();
  }

  @Bean
  public ProcessReport processReport() {
    return new FancyConsoleProcessReport();
  }
}
