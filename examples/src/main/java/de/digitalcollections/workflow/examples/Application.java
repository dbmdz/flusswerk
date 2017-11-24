package de.digitalcollections.workflow.examples;

import de.digitalcollections.workflow.engine.Engine;
import de.digitalcollections.workflow.engine.Flow;
import de.digitalcollections.workflow.engine.FlowBuilder;
import de.digitalcollections.workflow.engine.MessageBroker;
import de.digitalcollections.workflow.engine.MessageBrokerBuilder;
import de.digitalcollections.workflow.engine.exceptions.WorkflowSetupException;
import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

  private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

  public void run(String... args) throws WorkflowSetupException, IOException {
    MessageBroker messageBroker = new MessageBrokerBuilder()
        .hostName("localhost")
        .username("guest")
        .password("guest")
        .exchanges("testExchange", "testDlx")
        .build();

    Flow<String, String> flow = new FlowBuilder<String, String>()
        .read("someInputQueue", Message::getType)
        .transform(new UppercaseTransformer(true))
        .write("someOutputQueue", DefaultMessage::withType)
        .build();

    Engine engine = new Engine(messageBroker, flow);
    engine.createTestMessages(500);
    engine.start();
  }

  public static void main(String[] args) throws IOException, WorkflowSetupException {
    Application application = new Application();
    application.run(args);
  }

}