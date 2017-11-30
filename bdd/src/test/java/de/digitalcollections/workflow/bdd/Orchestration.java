package de.digitalcollections.workflow.bdd;

import de.digitalcollections.workflow.engine.Engine;
import de.digitalcollections.workflow.engine.Flow;
import de.digitalcollections.workflow.engine.MessageBroker;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Orchestration {

  private static Orchestration INSTANCE = null;

  private Engine engine;

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
    TimeUnit.SECONDS.sleep(1);
    return engine;
  }

  public void stopEngine() {
    if (engine != null) {
      engine.stop();
    }
  }

}
