package org.mdz.dzp.workflow.neo.testapp;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.mdz.dzp.workflow.neo.engine.Engine;
import org.mdz.dzp.workflow.neo.engine.Flow;
import org.mdz.dzp.workflow.neo.engine.FlowBuilder;
import org.mdz.dzp.workflow.neo.engine.MessageBroker;
import org.mdz.dzp.workflow.neo.engine.MessageBrokerBuilder;
import org.mdz.dzp.workflow.neo.engine.model.DefaultMessage;
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
        .exchanges("testExchange", "testDlx")
        .build();

    Flow<String, String> flow = new FlowBuilder<String, String>()
        .read("testQueue", Message::getType)
        .transform(new UppercaseTransformer(true))
        .write("output", DefaultMessage::new)

        .build();

    Engine engine = new Engine(messageBroker, flow);
    engine.createTestMessages(500);
    engine.start();
  }

  public static void main(String[] args) throws IOException, TimeoutException {
    Application application = new Application();
    application.run(args);
  }

}
