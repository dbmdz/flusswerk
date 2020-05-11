package com.github.dbmdz.flusswerk.spring.boot.starter;

import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.model.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/** Common base class for application main class to remove boilerplate. */
@SpringBootApplication
@EnableFlusswerk
public class FlusswerkApplication<M extends Message> implements CommandLineRunner {

  protected final Engine<M> engine;

  public FlusswerkApplication(@Qualifier("engine") Engine<M> engine) {
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
