package dev.mdz.flusswerk.integration.processing;

import static dev.mdz.flusswerk.integration.RabbitUtilAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import dev.mdz.flusswerk.config.FlusswerkConfiguration;
import dev.mdz.flusswerk.config.FlusswerkPropertiesConfiguration;
import dev.mdz.flusswerk.config.properties.RabbitMQProperties;
import dev.mdz.flusswerk.config.properties.RoutingProperties;
import dev.mdz.flusswerk.engine.Engine;
import dev.mdz.flusswerk.exceptions.RetryProcessingException;
import dev.mdz.flusswerk.flow.FlowSpec;
import dev.mdz.flusswerk.flow.builder.FlowBuilder;
import dev.mdz.flusswerk.integration.RabbitUtil;
import dev.mdz.flusswerk.integration.TestMessage;
import dev.mdz.flusswerk.model.IncomingMessageType;
import dev.mdz.flusswerk.model.Message;
import dev.mdz.flusswerk.rabbitmq.RabbitConnection;
import dev.mdz.flusswerk.rabbitmq.RabbitMQ;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
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
      RetryTest.FlowConfiguration.class,
    })
@Import({MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@DisplayName("When processing for a message fails")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
public class RetryTest {

  @Container
  static final RabbitMQContainer rabbitMQContainer =
      new RabbitMQContainer("rabbitmq:3-management-alpine");

  private final Engine engine;

  private final RabbitUtil rabbitUtil;

  @Autowired
  public RetryTest(Engine engine, RabbitMQ rabbitMQ, RoutingProperties routingProperties) {
    this.engine = engine;
    rabbitUtil = new RabbitUtil(rabbitMQ, routingProperties);
  }

  static class CountFailures implements Function<TestMessage, Message> {

    private final AtomicInteger count = new AtomicInteger();

    @Override
    public Message apply(TestMessage message) {
      throw new RetryProcessingException("Fail message to retry (%d)", count.incrementAndGet());
    }
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
      return FlowBuilder.messageProcessor(TestMessage.class).process(new CountFailures()).build();
    }
  }

  @BeforeEach
  void startEngine() {
    engine.start();
  }

  @AfterEach
  void stopEngine() {
    engine.stop();
    rabbitUtil.purgeQueues();
  }

  @Test
  @DisplayName("then Flusswerk should retry the message 5 times")
  void shouldRetryMessage() throws IOException {
    var message = new TestMessage("12345");

    rabbitUtil.send(message);

    var received = rabbitUtil.receiveFailed();

    assertThat(((TestMessage) received).getId()).isEqualTo(message.getId());
    assertThat(received.getEnvelope().getRetries())
        .isEqualTo(rabbitUtil.firstFailurePolicy().getRetries());
    assertThat(rabbitUtil).allQueuesAreEmpty();
  }
}
