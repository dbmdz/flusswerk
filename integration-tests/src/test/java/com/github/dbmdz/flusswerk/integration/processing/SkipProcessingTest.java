package com.github.dbmdz.flusswerk.integration.processing;

import static com.github.dbmdz.flusswerk.integration.RabbitUtilAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import com.github.dbmdz.flusswerk.framework.config.properties.RabbitMQProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.exceptions.SkipProcessingException;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitConnection;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import com.github.dbmdz.flusswerk.integration.RabbitUtil;
import com.github.dbmdz.flusswerk.integration.TestMessage;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ContextConfiguration(
    classes = {
      FlusswerkPropertiesConfiguration.class,
      FlusswerkConfiguration.class,
      SkipProcessingTest.FlowConfiguration.class,
      Tracing.class,
    })
@ComponentScan({
  "com.github.dbmdz.flusswerk.framework.rabbitmq",
  "com.github.dbmdz.flusswerk.framework.monitoring",
})
@Import({MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@DisplayName("When Flusswerk skips a message")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
public class SkipProcessingTest {

  @Container
  static RabbitMQContainer rabbitMQContainer =
      new RabbitMQContainer("rabbitmq:3-management-alpine");

  private final Engine engine;

  private final RabbitUtil rabbitUtil;

  @Autowired
  public SkipProcessingTest(Engine engine, RoutingProperties routingProperties, RabbitMQ rabbitMQ) {
    this.engine = engine;
    rabbitUtil = new RabbitUtil(rabbitMQ, routingProperties);
  }

  @TestConfiguration
  static class FlowConfiguration {
    @Bean
    @Primary
    public RabbitConnection rabbitConnection() throws IOException {
      return new RabbitConnection(
          new RabbitMQProperties(
              List.of(
                  String.format(
                      "%s:%d", rabbitMQContainer.getHost(), rabbitMQContainer.getAmqpPort())),
              "/",
              "guest",
              "guest"),
          "no-flow-test");
    }

    @Bean
    public IncomingMessageType incomingMessageType() {
      return new IncomingMessageType(TestMessage.class);
    }

    @Bean
    public FlowSpec flowSpec() {
      return FlowBuilder.flow(TestMessage.class, String.class)
          .reader(
              testMessage -> {
                System.out.println("Reader: " + testMessage);
                throw new SkipProcessingException("Skip processing for testing")
                    .send(new TestMessage("Skipping worked!"));
              })
          .transformer(
              string -> {
                System.out.println("Transformer: " + string);
                throw new RuntimeException(
                    "Skipping did not work: Transformer should not be called");
              })
          .writerSendingMessage(
              value -> {
                System.out.println("Writer: " + value);
                throw new RuntimeException("Skipping did not work: Writer should not be called");
              })
          .build();
    }
  }

  @BeforeEach
  void startEngine() {
    engine.start();
  }

  @AfterEach
  void stopEngine() throws IOException {
    engine.stop();
    rabbitUtil.purgeQueues();
  }

  @DisplayName("then the message should end up in the output queue")
  @Test
  void successfulMessagesShouldGoToOutQueue() throws Exception {

    rabbitUtil.send(new TestMessage("Test message"));

    var received = (TestMessage) rabbitUtil.receive();

    assertThat(received.getId()).isEqualTo("Skipping worked!");
    assertThat(rabbitUtil).allQueuesAreEmpty();
  }
}
