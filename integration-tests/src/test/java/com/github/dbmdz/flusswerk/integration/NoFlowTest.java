package com.github.dbmdz.flusswerk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import com.github.dbmdz.flusswerk.framework.config.properties.RabbitMQProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitConnection;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import com.github.dbmdz.flusswerk.integration.NoFlowTest.NoFlowTestConfiguration;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
      NoFlowTestConfiguration.class
    })
@Import({MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@DisplayName("When Flusswerk is created without a Flow")
@Testcontainers
public class NoFlowTest {
  @Container
  static final RabbitMQContainer rabbitMQContainer =
      new RabbitMQContainer("rabbitmq:3-management-alpine");

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

  private final Engine engine;
  private final RabbitMQ rabbitMQ;
  private final RabbitUtil rabbitUtil;

  @Autowired
  public NoFlowTest(Engine engine, RoutingProperties routingProperties, RabbitMQ rabbitMQ) {
    this.engine = engine;
    this.rabbitMQ = rabbitMQ;
    this.rabbitUtil = new RabbitUtil(rabbitMQ, routingProperties);
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
