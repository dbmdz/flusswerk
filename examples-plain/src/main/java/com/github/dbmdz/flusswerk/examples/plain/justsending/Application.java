package com.github.dbmdz.flusswerk.examples.plain.justsending;

import com.github.dbmdz.flusswerk.examples.plain.AppMessage;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.messagebroker.builder.MessageBrokerBuilder;
import com.github.dbmdz.flusswerk.framework.messagebroker.builder.RabbitMQ;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;

/** Example how to use Flusswerk just to send messages. */
public class Application {

  private void run() throws IOException {
    MessageBroker<Message> messageBroker =
        MessageBrokerBuilder.sendTo("some.output.queue")
            .via(RabbitMQ.host("localhost", 5672).auth("guest", "guest").exchange("workflow"))
            .build();

    for (int i = 0; i < 10; i++) {
      messageBroker.send(new AppMessage(Integer.toString(i)));
    }
  }

  public static void main(String[] args) throws IOException {
    Application application = new Application();
    application.run();
  }
}
