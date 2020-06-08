package com.github.dbmdz.flusswerk.examples.plain.job;

import com.github.dbmdz.flusswerk.examples.plain.AppMessage;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBrokerBuilder;
import java.io.IOException;

/** Example for a typical workflow job. */
public class Application {

  private void run() throws IOException {
    MessageBroker messageBroker =
        new MessageBrokerBuilder()
            .connectTo("localhost", 5672)
            .username("guest")
            .password("guest")
            .exchange("workflow")
            .deadLetterExchange("workflow.dlx")
            .readFrom("someInputQueue")
            .writeTo("someOutputQueue")
            .build();

    var flow =
        FlowBuilder.flow(AppMessage.class, String.class, String.class)
            .reader(AppMessage::getId)
            .transformer(new UppercaseTransformer(true))
            .writerSendingMessage(AppMessage::new)
            .build();

    Engine engine = new Engine("demo", messageBroker, flow);
    messageBroker.send("someInputQueue", new AppMessage("lowercase-text"));
    engine.start();
  }

  public static void main(String[] args) throws IOException {
    Application application = new Application();
    application.run();
  }
}
