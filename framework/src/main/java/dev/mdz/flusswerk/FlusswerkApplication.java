package dev.mdz.flusswerk;

import dev.mdz.flusswerk.engine.Engine;
import java.util.Optional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Common base class for application main class to remove boilerplate. */
@SpringBootApplication
@EnableFlusswerk
public class FlusswerkApplication implements CommandLineRunner {

  protected final Engine engine; // can be null if Flusswerk is only used to send messages

  public FlusswerkApplication() {
    this.engine = null; // for backwards compatibility, will be removed
  }

  @Deprecated
  public FlusswerkApplication(Optional<Engine> engine) {
    // for backwards compatibility, will be removed
    this.engine = engine.orElse(null);
  }

  @Override
  public void run(String... args) {
    // nothing to do
  }
}
