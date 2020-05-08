package com.github.dbmdz.flusswerk.examples.plain.job;

import com.github.dbmdz.flusswerk.examples.plain.AppMessage;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.messagebroker.builder.MessageBrokerBuilder;
import com.github.dbmdz.flusswerk.framework.messagebroker.builder.RabbitMQ;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Example for a typical workflow job. */
public class Application {

  private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

  private void run() throws IOException {
    MessageBroker<AppMessage> messageBroker =
        MessageBrokerBuilder.read(AppMessage.class)
            .from("someInputQueue")
            .sendTo("someOutputQueue")
            .via(
                RabbitMQ.host("localhost", 5672) // For multiple hosts use Address
                    .auth("guest", "guest") // guest/guest is RabbitMQ default
                    .exchange("workflow"))
            .build();

    var flow =
        FlowBuilder.flow(AppMessage.class, String.class, String.class)
            .reader(AppMessage::getId)
            .transformer(new UppercaseTransformer(true))
            .writerSendingMessage(AppMessage::new)
            .build();

    Engine<AppMessage> engine = new Engine<>(messageBroker, flow);
    messageBroker.send("someInputQueue", new AppMessage("lowercase-text"));
    engine.start();
  }

  public static void main(String[] args) throws IOException {
    Application application = new Application();
    application.run();
  }
}
