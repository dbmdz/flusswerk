package org.mdz.dzp.workflow.neo.testapp;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.mdz.dzp.workflow.neo.engine.Engine;
import org.mdz.dzp.workflow.neo.engine.Flow;
import org.mdz.dzp.workflow.neo.engine.FlowBuilder;
import org.mdz.dzp.workflow.neo.engine.MessageBroker;
import org.mdz.dzp.workflow.neo.engine.MessageBrokerBuilder;
import org.mdz.dzp.workflow.neo.engine.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

  private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

  public void run(String... args) throws IOException, TimeoutException {
    MessageBroker messageBroker = new MessageBrokerBuilder()
        .hostName("localhost")
        .username("guest")
        .password("guest")
        .build();

    Flow<String, String> flow = new FlowBuilder<String, String>()
        .read("testQueue", Message::getValue)
        .transform(this::doSomeTransformations)
        .write("output", Message::new)
        .exchange("testExchange")
        .deadLetterExchange("testDlx")
        .build();

    Engine engine = new Engine(messageBroker, flow);
    engine.createTestMessages();
    engine.start();
  }

  public String doSomeTransformations(String value) {
    try {
      TimeUnit.MILLISECONDS.sleep(250);
    } catch (InterruptedException e) {
      LOGGER.error("Sleep interrupted...", e);
    }
    return value;
  }

  public static void main(String[] args) throws IOException, TimeoutException {
    Application application = new Application();
    application.run(args);
  }

}
