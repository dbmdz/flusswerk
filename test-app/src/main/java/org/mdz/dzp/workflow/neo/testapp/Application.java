package org.mdz.dzp.workflow.neo.testapp;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.mdz.dzp.workflow.neo.engine.Engine;
import org.mdz.dzp.workflow.neo.engine.Flow;
import org.mdz.dzp.workflow.neo.engine.FlowBuilder;
import org.mdz.dzp.workflow.neo.engine.RabbitMQ;
import org.mdz.dzp.workflow.neo.engine.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

  private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

  public Application() {
  }

  public void run(String... args) throws IOException, TimeoutException {
    Flow<String, String> flow = new FlowBuilder<String, String>()
        .read("testQueue", Message::getValue)
        .transform(value -> {
          try {
            TimeUnit.MILLISECONDS.sleep(250);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          return value;
        })
        .write("output", Message::new)
        .build();

    RabbitMQ rabbitMQ = new RabbitMQ();
    Engine engine = new Engine(rabbitMQ, flow);
    engine.createTestMessages();
    engine.start();
  }

  public static void main(String[] args) throws IOException, TimeoutException {
    Application application = new Application();
    application.run(args);
  }

}
