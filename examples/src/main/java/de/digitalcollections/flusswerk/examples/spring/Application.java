package de.digitalcollections.flusswerk.examples.spring;

import de.digitalcollections.flusswerk.engine.Engine;
import de.digitalcollections.flusswerk.engine.exceptions.WorkflowSetupException;
import de.digitalcollections.flusswerk.engine.messagebroker.MessageBroker;
import de.digitalcollections.flusswerk.engine.model.DefaultMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application implements CommandLineRunner {

  private final MessageBroker messageBroker;

  private final Engine engine;

  @Autowired
  public Application(MessageBroker messageBroker, Engine engine) {
    this.engine = engine;
    this.messageBroker = messageBroker;
  }

  @Override
  public void run(String... strings) throws WorkflowSetupException, java.io.IOException {
    messageBroker.send("someInputQueue", new DefaultMessage("lowercase-text").put("text", "Shibuyara"));
    engine.start();
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
