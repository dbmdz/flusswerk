package com.github.dbmdz.flusswerk.integration.processing;

import static com.github.dbmdz.flusswerk.integration.RabbitUtilAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import com.github.dbmdz.flusswerk.framework.config.properties.RabbitMQProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.exceptions.RetryProcessingException;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitConnection;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import com.github.dbmdz.flusswerk.integration.RabbitUtil;
import com.github.dbmdz.flusswerk.integration.TestMessage;
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
