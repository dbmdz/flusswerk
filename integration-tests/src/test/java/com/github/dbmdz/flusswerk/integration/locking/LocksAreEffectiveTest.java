package com.github.dbmdz.flusswerk.integration.locking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.exceptions.LockingException;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.locking.LockManager;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import com.github.dbmdz.flusswerk.integration.ProcessorAdapter;
import com.github.dbmdz.flusswerk.integration.RabbitUtil;
import com.github.dbmdz.flusswerk.integration.TestMessage;
import com.github.dbmdz.flusswerk.integration.locking.LocksAreEffectiveTest.LocksAreEffectiveConfiguration;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import org.assertj.core.api.Condition;
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
      LocksAreEffectiveConfiguration.class,
      FlusswerkPropertiesConfiguration.class,
      FlusswerkConfiguration.class
    })
@Import({MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@DisplayName("When there are locks")
public class LocksAreEffectiveTest {

  private final Engine engine;

  private final RoutingProperties routing;

  private final ExecutorService executorService;

  private final RabbitUtil rabbitUtil;

  private final RabbitMQ rabbitMQ;

  private final ProcessorAdapter<LockTestingMessage> processorAdapter;

  private final LockManager lockManager;

  @Autowired
  public LocksAreEffectiveTest(
      Engine engine,
      RoutingProperties routingProperties,
      RabbitMQ rabbitMQ,
      ProcessorAdapter<LockTestingMessage> processorAdapter,
      LockManager lockManager) {
    this.engine = engine;
    this.processorAdapter = processorAdapter;
    this.routing = routingProperties;
    this.rabbitMQ = rabbitMQ;
    this.lockManager = lockManager;
    executorService = Executors.newSingleThreadExecutor();
    rabbitUtil = new RabbitUtil(rabbitMQ, routing);
  }

  static class LockTestingMessage extends Message {
    private final String id;
    private final boolean wasAlreadyLocked;
    private final long waitedForLockMs;

    @JsonCreator
    public LockTestingMessage(
        @JsonProperty("id") String id,
        @JsonProperty("wasAlreadyLocked") boolean wasAlreadyLocked,
        @JsonProperty("waitedForLockNs") long waitedForLockMs) {
      this.id = id;
      this.waitedForLockMs = waitedForLockMs;
      this.wasAlreadyLocked = wasAlreadyLocked;
    }

    public boolean getWasAlreadyLocked() {
      return wasAlreadyLocked;
    }

    public long getWaitedForLockMs() {
      return waitedForLockMs;
    }

    public String getId() {
      return id;
    }

    @Override
    public String toString() {
      return String.format(
          "LockTestingMessage{id=%s, wasAlreadyLocked=%s, waitedForLockMs=%,d}",
          id, wasAlreadyLocked, waitedForLockMs);
    }
  }

  @TestConfiguration
  static class LocksAreEffectiveConfiguration {
    @Bean
    public IncomingMessageType incomingMessageType() {
      return new IncomingMessageType(LockTestingMessage.class);
    }

    @Bean
    public ProcessorAdapter<LockTestingMessage> processorAdapter() {
      return new ProcessorAdapter<>();
    }

    @Bean
    public FlowSpec flowSpec(ProcessorAdapter<LockTestingMessage> processorAdapter) {
      return FlowBuilder.messageProcessor(LockTestingMessage.class)
          .process(processorAdapter)
          .build();
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

  public static Condition<LockTestingMessage> waitedAtLeast(long nanoseconds) {
    return new Condition<>(
        message -> message.getWaitedForLockMs() >= nanoseconds,
        "waited at least %d ns",
        nanoseconds);
  }

  public static Condition<LockTestingMessage> waitedAtMost(long nanoseconds) {
    return new Condition<>(
        message -> message.getWaitedForLockMs() < nanoseconds,
        "waited at least %d ns",
        nanoseconds);
  }

  @DisplayName("then these locks block further processing until released")
  @Test
  public void testLocksAreEffective() throws Exception {
    var inputQueue = routing.getIncoming().get(0);
    var outputQueue = routing.getOutgoing().get("default");

    rabbitMQ.topic(inputQueue).send(new TestMessage("1"));
    rabbitMQ.topic(inputQueue).send(new TestMessage("2"));

    String id = "123";
    long expectedWaitingTimeMs = 1000;

    Semaphore waitForThreadOneGettingLock = new Semaphore(1);
    waitForThreadOneGettingLock.drainPermits();

    processorAdapter.setFunction(
        message -> {
          if ("2".equals(message.getId())) {
            try {
              waitForThreadOneGettingLock.acquire();
            } catch (InterruptedException e) {
              fail("Could not wait for ");
            }
          }
          boolean alreadyLocked = lockManager.isLocked(id);
          long startWaiting = System.nanoTime();
          try {
            lockManager.acquire(id);
            if ("1".equals(message.getId())) {
              waitForThreadOneGettingLock.release();
            }
          } catch (LockingException e) {
            fail("Could not acquire lock", e);
          }
          long waitedForLockMs = (System.nanoTime() - startWaiting) / 1000;

          try {
            Thread.sleep(expectedWaitingTimeMs);
          } catch (InterruptedException e) {
            fail("Could not wait enough to test locks", e);
          }

          return new LockTestingMessage(message.getId(), alreadyLocked, waitedForLockMs);
        });

    var backoff = routing.getFailurePolicy(inputQueue).getBackoff();
    var messages =
        List.of(
            (LockTestingMessage) rabbitUtil.waitAndAck(outputQueue, backoff),
            (LockTestingMessage) rabbitUtil.waitAndAck(outputQueue, backoff));

    assertThat(messages).filteredOn("tracingId", "1").have(waitedAtMost(1000_000));

    assertThat(messages).filteredOn("tracingId", "2").have(waitedAtLeast(1000_000));
  }
}
