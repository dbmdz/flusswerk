package com.github.dbmdz.flusswerk.examples.plain.job;

import com.github.dbmdz.flusswerk.framework.Engine;
import com.github.dbmdz.flusswerk.framework.exceptions.WorkflowSetupException;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.flow.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBrokerBuilder;
import com.github.dbmdz.flusswerk.framework.model.DefaultMessage;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import java.util.function.Function;
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

    Flow<DefaultMessage, String, String> flow =
        new FlowBuilder<DefaultMessage, String, String>()
            .read(DefaultMessage::getId)
            .transform(new UppercaseTransformer(true))
            .writeAndSend((Function<String, Message>) DefaultMessage::new)
            .build();

    Engine engine = new Engine(messageBroker, flow);
    messageBroker.send(
        "someInputQueue", new DefaultMessage("lowercase-text").put("text", "Shibuyara"));
    engine.start();
  }

  public static void main(String[] args) throws IOException, WorkflowSetupException {
    Application application = new Application();
    application.run();
  }
}
