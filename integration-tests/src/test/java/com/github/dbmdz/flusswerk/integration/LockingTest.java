package com.github.dbmdz.flusswerk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import com.github.dbmdz.flusswerk.framework.config.properties.RedisProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.locking.LockManager;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import com.github.dbmdz.flusswerk.integration.SuccessfulProcessingTest.FlowConfiguration;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.api.RLock;
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
public class LockingTest {

  private static final Condition<? super RLock> LOCKED = new Condition<>(RLock::isLocked, "locked");

  private static final Condition<? super RLock> UNLOCKED =
      new Condition<>(lock -> !lock.isLocked(), "locked");

  private final Engine engine;

  private final RoutingProperties routing;

  private final ExecutorService executorService;

  private final RabbitUtil rabbitUtil;

  private final RabbitMQ rabbitMQ;

  private final RedisUtil redisUtil;

  private final ProcessorAdapter<Message> processorAdapter;

  private final LockManager lockManager;

  @Autowired
  public LockingTest(
      Engine engine,
      RoutingProperties routingProperties,
      RedisProperties redisProperties,
      RabbitMQ rabbitMQ,
      ProcessorAdapter<Message> processorAdapter,
      LockManager lockManager) {
    this.engine = engine;
    this.processorAdapter = processorAdapter;
    this.routing = routingProperties;
    this.rabbitMQ = rabbitMQ;
    this.redisUtil = new RedisUtil(redisProperties);
    this.lockManager = lockManager;
    executorService = Executors.newSingleThreadExecutor();
    rabbitUtil = new RabbitUtil(rabbitMQ, routing);
  }

  /**
   * Adapter class so that tests can inject test logic while we still can use Spring Test Autowiring
   * setup.
   *
   * @param <T> Message or a subclass of Message - the incoming message type.
   */
  static class ProcessorAdapter<T extends Message> implements Function<T, Message> {

    private Function<T, Message> function;

    @Override
    public Message apply(T message) {
      if (function == null) {
        throw new RuntimeException("Processor called before actual function was assigned");
      }
      return function.apply(message);
    }

    public void setFunction(Function<T, Message> function) {
      this.function = function;
    }
  }

  @TestConfiguration
  static class FlowConfiguration {

    @Bean
    public ProcessorAdapter<Message> processorAdapter() {
      return new ProcessorAdapter<>();
    }

    @Bean
    public FlowSpec flowSpec(ProcessorAdapter<Message> processorAdapter) {
      return FlowBuilder.messageProcessor(Message.class).process(processorAdapter).build();
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

    String id = "123";
    processorAdapter.setFunction(
        message -> {
          assertThat(redisUtil.getRLock(id)).is(UNLOCKED);
          try {
            lockManager.acquire(id);
          } catch (InterruptedException e) {
            fail("Could not acquire lock", e);
          }
          assertThat(redisUtil.getRLock(id)).is(LOCKED);
          return message;
        });

    var received =
        rabbitUtil.waitForMessage(outputQueue, failurePolicy, this.getClass().getSimpleName());
    rabbitMQ.ack(received);

    assertThat(redisUtil.getRLock(id)).is(UNLOCKED);
  }
}
