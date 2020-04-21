package com.github.dbmdz.flusswerk.spring.boot.starter;

import com.github.dbmdz.flusswerk.framework.Engine;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/** Common base class for application main class to remove boilerplate. */
@SpringBootApplication
@EnableFlusswerk
public class FlusswerkApplication implements CommandLineRunner {

  protected Engine engine;

  public FlusswerkApplication(Engine engine) {
    this.engine = engine;
  }

  @Override
  public void run(String... args) {
    engine.start();
  }

  public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
    return SpringApplication.run(primarySource, args);
  }
}
