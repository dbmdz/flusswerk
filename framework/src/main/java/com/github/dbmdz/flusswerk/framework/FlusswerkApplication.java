package com.github.dbmdz.flusswerk.framework;

import com.github.dbmdz.flusswerk.framework.engine.Engine;
import java.util.Optional;
import javax.annotation.PreDestroy;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Common base class for application main class to remove boilerplate. */
@SpringBootApplication
@EnableFlusswerk
public class FlusswerkApplication implements CommandLineRunner {

  protected final Engine engine; // can be null if Flusswerk is only used to send messages

  public FlusswerkApplication(Optional<Engine> engine) {
    this.engine = engine.orElse(null);
  }

  @Override
  public void run(String... args) {
    if (engine == null) {
      return;
    }
    engine.start();
  }

  @PreDestroy
  public void shutdown() {
    if (engine == null) {
      return;
    }
    engine.stop();
  }
}
