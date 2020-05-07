package com.github.dbmdz.flusswerk.examples.plain.job;

import com.github.dbmdz.flusswerk.examples.plain.AppMessage;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.flow.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBrokerBuilder;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Example for a typical workflow job. */
public class Application {

  private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

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

    Flow<AppMessage, String, String> flow =
        new FlowBuilder<AppMessage, String, String>()
            .read(AppMessage::getId)
            .transform(new UppercaseTransformer(true))
            .writeAndSend(AppMessage::new)
            .build();

    Engine engine = new Engine(messageBroker, flow);
    messageBroker.send("someInputQueue", new AppMessage("lowercase-text"));
    engine.start();
  }

  public static void main(String[] args) throws IOException {
    Application application = new Application();
    application.run();
  }
}
