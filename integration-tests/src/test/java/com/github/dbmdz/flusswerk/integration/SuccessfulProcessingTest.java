package com.github.dbmdz.flusswerk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import com.github.dbmdz.flusswerk.framework.config.properties.FlusswerkProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.rabbitmq.MessageBroker;
import com.github.dbmdz.flusswerk.framework.rabbitmq.Queues;
import com.github.dbmdz.flusswerk.framework.model.Message;
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

  private final MessageBroker messageBroker;

  private final Queues queues;

  private final RoutingProperties routing;

  private final ExecutorService executorService;

  private final RabbitMQ rabbitMQ;

  @Autowired
  public SuccessfulProcessingTest(
      Engine engine,
      MessageBroker messageBroker,
      Queues queues,
      FlusswerkProperties flusswerkProperties) {
    this.engine = engine;
    this.messageBroker = messageBroker;
    this.queues = queues;
    this.routing = flusswerkProperties.getRouting();
    executorService = Executors.newSingleThreadExecutor();
    rabbitMQ = new RabbitMQ(messageBroker, queues, routing);
  }

  @TestConfiguration
  static class FlowConfiguration {
    @Bean
    public FlowSpec<Message, Message, Message> flowSpec() {
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
    rabbitMQ.purgeQueues();
  }

  @Test
  public void successfulMessagesShouldGoToOutQueue() throws Exception {
    var inputQueue = routing.getReadFrom().get(0);
    var outputQueue = routing.getWriteTo().orElseThrow();
    var failurePolicy = routing.getFailurePolicy(inputQueue);

    Message expected = new Message("123456");
    messageBroker.send(inputQueue, expected);

    var received =
        rabbitMQ.waitForMessage(outputQueue, failurePolicy, this.getClass().getSimpleName());
    assertThat(received.getTracingId()).isEqualTo(expected.getTracingId());

    assertThat(queues.messageCount(inputQueue)).isZero();
    assertThat(queues.messageCount(outputQueue)).isZero();
    assertThat(queues.messageCount(failurePolicy.getRetryRoutingKey())).isZero();
    assertThat(queues.messageCount(failurePolicy.getFailedRoutingKey())).isZero();
  }
}
