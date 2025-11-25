package dev.mdz.flusswerk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import dev.mdz.flusswerk.config.FlusswerkConfiguration;
import dev.mdz.flusswerk.config.FlusswerkPropertiesConfiguration;
import dev.mdz.flusswerk.config.properties.RabbitMQProperties;
import dev.mdz.flusswerk.config.properties.RoutingProperties;
import dev.mdz.flusswerk.engine.Engine;
import dev.mdz.flusswerk.model.IncomingMessageType;
import dev.mdz.flusswerk.model.Message;
import dev.mdz.flusswerk.rabbitmq.RabbitConnection;
import dev.mdz.flusswerk.rabbitmq.RabbitMQ;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
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
      NoFlowTest.NoFlowTestConfiguration.class
    })
@Import(IntegrationTestConfiguration.class)
@DisplayName("When Flusswerk is created without a Flow")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
public class NoFlowTest {
  @Container
  static final RabbitMQContainer rabbitMQContainer =
      new RabbitMQContainer("rabbitmq:4-management-alpine");

  private final Engine engine;
  private final RabbitMQ rabbitMQ;
  private final RabbitUtil rabbitUtil;

  @Autowired
  public NoFlowTest(
      Optional<Engine> engine, RoutingProperties routingProperties, RabbitMQ rabbitMQ) {
    this.engine = engine.orElse(null);
    this.rabbitMQ = rabbitMQ;
    this.rabbitUtil = new RabbitUtil(rabbitMQ, routingProperties);
  }

  @TestConfiguration
  static class NoFlowTestConfiguration {
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
  }

  @AfterEach
  void stopEngine() {
    rabbitUtil.purgeQueues();
  }

  @DisplayName("then it still should send a message to a route")
  @Test
  public void shouldSendMessageToRoute() {
    Message message = new TestMessage("123");
    rabbitMQ.topic(rabbitUtil.output()).send(message);
    Message received = rabbitUtil.receive();
    assertThat(received).isEqualTo(message);
  }

  @DisplayName("then it should not create an instance of Engine")
  @Test
  public void shouldNotCreateAnInstanceOfEngine() {
    assertThat(engine).isNull();
  }
}
