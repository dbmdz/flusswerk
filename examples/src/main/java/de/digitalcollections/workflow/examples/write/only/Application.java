package de.digitalcollections.workflow.examples.write.only;

import de.digitalcollections.workflow.engine.exceptions.WorkflowSetupException;
import de.digitalcollections.workflow.engine.messagebroker.MessageBroker;
import de.digitalcollections.workflow.engine.messagebroker.MessageBrokerBuilder;
import de.digitalcollections.workflow.engine.model.DefaultMessage;
import java.io.IOException;

public class Application {

  private void run() throws IOException {
    MessageBroker messageBroker = new MessageBrokerBuilder()
        .exchange("workflow")
        .writeTo("someOutputQueue")
        .build();

    for (int i=0; i < 10; i++) {
      messageBroker.send(new DefaultMessage(Integer.toString(i)));
    }

  }

  public static void main(String[] args) throws IOException, WorkflowSetupException {
    Application application = new Application();
    application.run();
  }
}
