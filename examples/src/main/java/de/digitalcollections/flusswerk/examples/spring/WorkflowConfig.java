package de.digitalcollections.flusswerk.examples.spring;

import de.digitalcollections.flusswerk.engine.Engine;
import de.digitalcollections.flusswerk.engine.flow.Flow;
import de.digitalcollections.flusswerk.engine.flow.FlowBuilder;
import de.digitalcollections.flusswerk.engine.messagebroker.MessageBroker;
import de.digitalcollections.flusswerk.engine.messagebroker.MessageBrokerBuilder;
import de.digitalcollections.flusswerk.engine.model.DefaultMessage;
import de.digitalcollections.flusswerk.engine.model.Message;
import java.io.IOException;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowConfig {

  @Bean
  public Function<DefaultMessage, String> reader() {
    return new StringReader();
  }

  @Bean
  public Function<String, String> transformer() {
    return new UppercaseTransformer();
  }

  @Bean
  public Function<String, Message> writer() {
    return new StringWriter();
  }

  @Bean
  public Flow flow() {
    return new FlowBuilder<DefaultMessage, String, String>()
        .read(reader())
        .transform(transformer())
        .writeAndSend(writer())
        .build();
  }

  @Bean
  public MessageBroker messageBroker() {
    return new MessageBrokerBuilder()
        .connectTo("localhost", 5672)
        .username("guest")
        .password("guest")
        .exchange("workflow")
        .deadLetterExchange("workflow.dlx")
        .readFrom("someInputQueue")
        .writeTo("someOutputQueue")
        .build();
  }

  @Bean
  public Engine engine() throws IOException {
    return new Engine(messageBroker(), flow());
  }

}
