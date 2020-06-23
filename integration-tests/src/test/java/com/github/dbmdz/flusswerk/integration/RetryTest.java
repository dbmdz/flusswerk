package com.github.dbmdz.flusswerk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import com.github.dbmdz.flusswerk.framework.config.properties.FlusswerkProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.Routing;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.exceptions.RetryProcessingException;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.messagebroker.Queues;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.integration.RetryTest.FlowConfiguration;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class RetryTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(RetryTest.class);

  private final Engine engine;

  private final MessageBroker messageBroker;

  private final Queues queues;

  private final Routing routing;

  private final ExecutorService executorService;

  private final RabbitMQ rabbitMQ;

  @Autowired
  public RetryTest(
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

  static class CountFailures implements UnaryOperator<Message> {

    private final AtomicInteger count = new AtomicInteger();

    @Override
    public Message apply(Message message) {
      throw new RetryProcessingException("Fail message to retry ({})", count.incrementAndGet());
    }
  }

  @TestConfiguration
  static class FlowConfiguration {

    @Bean
    public FlowSpec<Message, Message, Message> flowSpec() {
      return FlowBuilder.messageProcessor(Message.class).process(new CountFailures()).build();
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

    //    // Cleanup leftover messages to not pollute other tests
    //    var readFrom = routing.getReadFrom();
    //    for (String queue : readFrom) {
    //      purge(queue);
    //      var failurePolicy = routing.getFailurePolicy(queue);
    //      purge(failurePolicy.getFailedRoutingKey()); // here routing key == queue name
    //      purge(failurePolicy.getRetryRoutingKey()); // here routing key == queue name
    //    }
    //
    //    // TODO really necessary?
    //    var writeTo = routing.getWriteTo();
    //    if (writeTo.isPresent()) {
    //      purge(writeTo.get());
    //    }
  }

  private void purge(String queue) throws IOException {
    var deletedMessages = queues.purge(queue);
    if (deletedMessages != 0) {
      LOGGER.error("Purged {} and found {} messages.", queue, deletedMessages);
    }
  }

  @Test
  @DisplayName("should retry message 5 times")
  void shouldRetryMessage() throws IOException, InvalidMessageException, InterruptedException {
    var message = new Message("12345");

    var inputQueue = routing.getReadFrom().get(0);
    var failurePolicy = routing.getFailurePolicy(inputQueue);

    messageBroker.send(inputQueue, message);
    var received = messageBroker.receive(failurePolicy.getFailedRoutingKey());
    var attempts = 0;
    while (received == null) {
      if (attempts > 50) {
        Assert.fail("To many attempts to receive message");
      }
      Thread.sleep(failurePolicy.getBackoff().toMillis()); // dead letter backoff time is 1s
      received = messageBroker.receive(failurePolicy.getFailedRoutingKey());
      attempts++;
      LOGGER.info(
          "Receive message attempt {}, got {} ({} messages in failed, {} messages in retry)",
          attempts,
          received != null ? "message" : "nothing",
          queues.messageCount(failurePolicy.getFailedRoutingKey()),
          queues.messageCount(failurePolicy.getRetryRoutingKey()));
    }
    assertThat(received.getTracingId()).isEqualTo(message.getTracingId());
    assertThat(received.getEnvelope().getRetries()).isEqualTo(failurePolicy.getMaxRetries());
  }
}
