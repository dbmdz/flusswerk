package de.digitalcollections.workflow.examples.spring;

import de.digitalcollections.workflow.engine.Engine;
import de.digitalcollections.workflow.engine.exceptions.WorkflowSetupException;
import de.digitalcollections.workflow.engine.messagebroker.MessageBroker;
import de.digitalcollections.workflow.engine.model.DefaultMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application implements CommandLineRunner {

  private MessageBroker messageBroker;

  private Engine engine;

  @Autowired
  public Application(MessageBroker messageBroker, Engine engine) {
    this.engine = engine;
    this.messageBroker = messageBroker;
  }

  @Override
  public void run(String... strings) throws WorkflowSetupException, java.io.IOException {
    messageBroker.send("someInputQueue", DefaultMessage.withType("lowercase-text").put("text", "Shibuyara"));
    engine.start();
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
