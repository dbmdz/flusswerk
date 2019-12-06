package de.digitalcollections.flusswerk.spring.boot.example;

import de.digitalcollections.flusswerk.engine.Engine;
import de.digitalcollections.flusswerk.spring.boot.starter.EnableFlusswerk;
import de.digitalcollections.flusswerk.spring.boot.starter.FlusswerkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableFlusswerk
public class Application implements ApplicationRunner {

  private static Logger LOGGER = LoggerFactory.getLogger(Application.class);

  private FlusswerkProperties flusswerkProperties;

  private Engine engine;

  @Autowired
  public Application(
      FlusswerkProperties flusswerkProperties, Engine engine) {
    this.flusswerkProperties = flusswerkProperties;
    this.engine = engine;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    LOGGER.info("Starting Spring Boot Demo with configuration:\n\n{}",
        flusswerkProperties.toString());
    engine.start();
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
