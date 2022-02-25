package com.github.dbmdz.flusswerk.integration.processing;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import com.github.dbmdz.flusswerk.integration.RabbitUtil;
import com.github.dbmdz.flusswerk.integration.TestMessage;
import com.github.dbmdz.flusswerk.integration.processing.SuccessfulProcessingTest.FlowConfiguration;
import java.io.IOException;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(
    classes = {
      FlowConfiguration.class,
      FlusswerkPropertiesConfiguration.class,
      FlusswerkConfiguration.class
    })
@Import({MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@DisplayName("When Flusswerk successfully processes a message")
public class SuccessfulProcessingTest {

  private final Engine engine;

  private final RoutingProperties routing;

  private final RabbitUtil rabbitUtil;

  private final RabbitMQ rabbitMQ;

  @Autowired
  public SuccessfulProcessingTest(
      Engine engine, RoutingProperties routingProperties, RabbitMQ rabbitMQ) {
    this.engine = engine;
    this.routing = routingProperties;
    this.rabbitMQ = rabbitMQ;
    rabbitUtil = new RabbitUtil(rabbitMQ, routing);
  }

  @TestConfiguration
  static class FlowConfiguration {

    @Bean
    public IncomingMessageType incomingMessageType() {
      return new IncomingMessageType(TestMessage.class);
    }

    @Bean
    public FlowSpec flowSpec() {
      return FlowBuilder.messageProcessor(TestMessage.class).process(m -> m).build();
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
  public void successfulMessagesShouldGoToOutQueue() throws Exception {
    var inputQueue = routing.getIncoming().get(0);
    var outputQueue = routing.getOutgoing().get("default");
    var failurePolicy = routing.getFailurePolicy(inputQueue);

    TestMessage expected = new TestMessage("123456");
    rabbitMQ.topic(inputQueue).send(expected);

    var received =
        rabbitUtil.waitForMessage(outputQueue, failurePolicy, this.getClass().getSimpleName());
    rabbitMQ.ack(received);
    assertThat(((TestMessage) received).getId()).isEqualTo(expected.getId());

    assertThat(rabbitMQ.queue(inputQueue).messageCount()).isZero();
    assertThat(rabbitMQ.queue(outputQueue).messageCount()).isZero();
    assertThat(rabbitMQ.queue(failurePolicy.getRetryRoutingKey()).messageCount()).isZero();
    assertThat(rabbitMQ.queue(failurePolicy.getFailedRoutingKey()).messageCount()).isZero();
  }
}
