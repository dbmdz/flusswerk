package com.github.dbmdz.flusswerk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import com.github.dbmdz.flusswerk.framework.config.properties.FlusswerkProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import com.github.dbmdz.flusswerk.integration.SuccessfulProcessingTest.FlowConfiguration;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
public class SuccessfulProcessingTest {

  private final Engine engine;

  private final RoutingProperties routing;

  private final ExecutorService executorService;

  private final RabbitUtil rabbitUtil;

  private final RabbitMQ rabbitMQ;

  @Autowired
  public SuccessfulProcessingTest(
      Engine engine, FlusswerkProperties flusswerkProperties, RabbitMQ rabbitMQ) {
    this.engine = engine;
    this.routing = flusswerkProperties.getRouting();
    this.rabbitMQ = rabbitMQ;
    executorService = Executors.newSingleThreadExecutor();
    rabbitUtil = new RabbitUtil(rabbitMQ, routing);
  }

  @TestConfiguration
  static class FlowConfiguration {
    @Bean
    public FlowSpec flowSpec() {
      return FlowBuilder.messageProcessor(Message.class).process(m -> m).build();
    }
  }

  @BeforeEach
  void startEngine() {
    executorService.submit(engine::start);
  }

  @AfterEach
  void stopEngine() throws IOException {
    engine.stop();
    rabbitUtil.purgeQueues();
  }

  @Test
  public void successfulMessagesShouldGoToOutQueue() throws Exception {
    var inputQueue = routing.getIncoming().get(0);
    var outputQueue = routing.getOutgoing().get("default");
    var failurePolicy = routing.getFailurePolicy(inputQueue);

    Message expected = new Message("123456");
    rabbitMQ.topic(inputQueue).send(expected);

    var received =
        rabbitUtil.waitForMessage(outputQueue, failurePolicy, this.getClass().getSimpleName());
    rabbitMQ.ack(received);
    assertThat(received.getTracingId()).isEqualTo(expected.getTracingId());

    assertThat(rabbitMQ.queue(inputQueue).messageCount()).isZero();
    assertThat(rabbitMQ.queue(outputQueue).messageCount()).isZero();
    assertThat(rabbitMQ.queue(failurePolicy.getRetryRoutingKey()).messageCount()).isZero();
    assertThat(rabbitMQ.queue(failurePolicy.getFailedRoutingKey()).messageCount()).isZero();
  }
}
