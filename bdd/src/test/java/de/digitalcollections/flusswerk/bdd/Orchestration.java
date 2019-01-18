package de.digitalcollections.flusswerk.bdd;

import de.digitalcollections.flusswerk.engine.Engine;
import de.digitalcollections.flusswerk.engine.exceptions.WorkflowSetupException;
import de.digitalcollections.flusswerk.engine.flow.Flow;
import de.digitalcollections.flusswerk.engine.messagebroker.MessageBroker;
import de.digitalcollections.flusswerk.engine.messagebroker.MessageBrokerBuilder;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

public class Orchestration {

  private static final Logger LOGGER = LoggerFactory.getLogger(Orchestration.class);

  private static Orchestration INSTANCE = null;

  private Engine engine;

  private GenericContainer rabbitMQ;

  public Orchestration() {
    rabbitMQ = new GenericContainer("rabbitmq:latest")
            .withExposedPorts(5672);
  }

  public static Orchestration getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new Orchestration();
    }
    return INSTANCE;
  }

  public Engine startEngine(MessageBroker messageBroker, Flow flow) throws IOException, InterruptedException {
    engine = new Engine(messageBroker, flow);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(engine::start);
//    TimeUnit.SECONDS.sleep(1);
    return engine;
  }

  public void stopEngine() {
    if (engine != null) {
      engine.stop();
    }
  }

  public GenericContainer getRabbitMQ() {
    return rabbitMQ;
  }

  public MessageBroker createMessageBroker(MessageBrokerBuilder messageBrokerBuilder) throws WorkflowSetupException {
    long totalWait = 0;
    long maxWait = 60 * 1000;
    long interval = 100;

    MessageBroker messageBroker = null;
    messageBrokerBuilder.connectTo("localhost", rabbitMQ.getMappedPort(5672));

    while ((messageBroker == null) && (totalWait <= maxWait)) {
      try {
        messageBroker = messageBrokerBuilder.build();
      } catch (RuntimeException e) {
        try {
          TimeUnit.MILLISECONDS.sleep(interval);
          totalWait += interval;
        } catch (InterruptedException e1) {
          throw new IllegalStateException(e1);
        }
      }
    }
    if (maxWait < totalWait) {
      throw new IllegalStateException("Waited for " + totalWait / 1000 + " seconds but got no suitable connection to RabbitMQ");
    }

    return messageBroker;
  }

}
