package com.github.dbmdz.flusswerk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import com.github.dbmdz.flusswerk.integration.NoFlowTest.NoFlowTestConfiguration;
import java.io.IOException;
import java.time.Duration;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(
    classes = {
      FlusswerkPropertiesConfiguration.class,
      FlusswerkConfiguration.class,
      NoFlowTestConfiguration.class
    })
@Import({MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@DisplayName("When Flusswerk is created without a Flow")
public class NoFlowTest {

  @TestConfiguration
  static class NoFlowTestConfiguration {
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
  void stopEngine() throws IOException {
    rabbitUtil.purgeQueues();
  }

  @DisplayName("then it still should send a message to a route")
  @Test
  public void shouldSendMessageToRoute()
      throws IOException, InterruptedException, InvalidMessageException {
    Message message = new TestMessage("123");
    rabbitMQ.route("default").send(message);
    Message received = rabbitUtil.waitAndAck("target.queue", Duration.ofMillis(50));
    assertThat(received).isEqualTo(message);
  }

  @DisplayName("then it should not create an instance of Engine")
  @Test
  public void shouldNotCreateAnInstanceOfEngine() {
    assertThat(engine).isNull();
  }
}
