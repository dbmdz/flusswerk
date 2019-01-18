package de.digitalcollections.flusswerk.examples.write.only;

import de.digitalcollections.flusswerk.engine.exceptions.WorkflowSetupException;
import de.digitalcollections.flusswerk.engine.messagebroker.MessageBroker;
import de.digitalcollections.flusswerk.engine.messagebroker.MessageBrokerBuilder;
import de.digitalcollections.flusswerk.engine.model.DefaultMessage;
import java.io.IOException;

public class Application {

  private void run() throws IOException {
    MessageBroker messageBroker = new MessageBrokerBuilder()
            .exchange("workflow")
            .writeTo("someOutputQueue")
            .build();

    for (int i = 0; i < 10; i++) {
      messageBroker.send(new DefaultMessage(Integer.toString(i)));
    }

  }

  public static void main(String[] args) throws IOException, WorkflowSetupException {
    Application application = new Application();
    application.run();
  }
}
