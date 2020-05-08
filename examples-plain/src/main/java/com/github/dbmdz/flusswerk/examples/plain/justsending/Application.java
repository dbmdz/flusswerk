package com.github.dbmdz.flusswerk.examples.plain.justsending;

import com.github.dbmdz.flusswerk.examples.plain.AppMessage;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBrokerBuilderOld;
import java.io.IOException;

/** Example how to use Flusswerk just to send messages. */
public class Application {

  private void run() throws IOException {
    MessageBroker messageBroker =
        new MessageBrokerBuilderOld().exchange("workflow").writeTo("someOutputQueue").build();

    for (int i = 0; i < 10; i++) {
      messageBroker.send(new AppMessage(Integer.toString(i)));
    }
  }

  public static void main(String[] args) throws IOException {
    Application application = new Application();
    application.run();
  }
}
