package de.digitalcollections.flusswerk.spring.boot.example.config;

import de.digitalcollections.flusswerk.engine.flow.Flow;
import de.digitalcollections.flusswerk.engine.flow.FlowBuilder;
import de.digitalcollections.flusswerk.engine.model.DefaultMessage;
import de.digitalcollections.flusswerk.engine.reporting.ProcessReport;
import de.digitalcollections.flusswerk.spring.boot.example.ComposePerfectGreeting;
import de.digitalcollections.flusswerk.spring.boot.example.FancyConsoleProcessReport;
import de.digitalcollections.flusswerk.spring.boot.example.model.Greeting;
import de.digitalcollections.flusswerk.spring.boot.starter.MessageImplementation;
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
  public Flow<DefaultMessage, String, String> flow() {
    return new FlowBuilder<DefaultMessage, String, String>()
        .read(message -> message.get("name"))
        .transform(new ComposePerfectGreeting())
        .write((Consumer<String>) System.out::println)
        .build();
  }

  @Bean
  public ProcessReport processReport() {
    return new FancyConsoleProcessReport();
  }
}
