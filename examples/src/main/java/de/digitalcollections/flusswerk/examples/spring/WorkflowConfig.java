package de.digitalcollections.flusswerk.examples.spring;

import de.digitalcollections.flusswerk.engine.Engine;
import de.digitalcollections.flusswerk.engine.flow.Flow;
import de.digitalcollections.flusswerk.engine.flow.FlowBuilder;
import de.digitalcollections.flusswerk.engine.messagebroker.MessageBroker;
import de.digitalcollections.flusswerk.engine.messagebroker.MessageBrokerBuilder;
import de.digitalcollections.flusswerk.engine.model.DefaultMessage;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowConfig {

  @Value("${spring.rabbitmq.host}")
  private String rabbitMQHost;

  @Value("${spring.rabbitmq.port}")
  private int rabbitMQPort;

  @Value("${spring.rabbitmq.username}")
  private String rabbitMQUsername;

  @Value("${spring.rabbitmq.password}")
  private String rabbitMQPassword;

  @Value("${spring.rabbitmq.template.exchange}")
  private String rabbitMQExchange;

  @Value("${messageBroker.deadLetterExchange}")
  private String deadLetterExchange;

  @Value("${messageBroker.queues.read}")
  private String inputQueue;

  @Value("${messageBroker.queues.write}")
  private String outputQueue;

  @Bean
  public Flow flow(StringReader reader, UppercaseTransformer transformer, StringWriter writer) {
    return new FlowBuilder<DefaultMessage, String, String>()
        .read(reader)
        .transform(transformer)
        .writeAndSend(writer)
        .build();
  }

  @Bean
  public MessageBroker messageBroker() {
    return new MessageBrokerBuilder()
        .connectTo(rabbitMQHost, rabbitMQPort)
        .username(rabbitMQUsername)
        .password(rabbitMQPassword)
        .exchange(rabbitMQExchange)
        .deadLetterExchange(deadLetterExchange)
        .readFrom(inputQueue)
        .writeTo(outputQueue)
        .build();
  }

  @Bean
  public Engine engine(MessageBroker messageBroker, Flow flow) throws IOException {
    return new Engine(messageBroker, flow);
  }
}
