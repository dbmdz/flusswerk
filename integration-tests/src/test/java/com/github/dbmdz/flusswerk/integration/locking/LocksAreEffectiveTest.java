package com.github.dbmdz.flusswerk.integration.locking;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import com.github.dbmdz.flusswerk.framework.config.properties.RedisProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.locking.LockManager;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import com.github.dbmdz.flusswerk.integration.ProcessorAdapter;
import com.github.dbmdz.flusswerk.integration.TestMessage;
import com.github.dbmdz.flusswerk.integration.WorkflowFixture;
import com.github.dbmdz.flusswerk.integration.locking.LocksAreEffectiveTest.LocksAreEffectiveConfiguration;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
@Disabled("Feature wil be removed until there is a more reliable implementation")
public class LocksAreEffectiveTest {

  private static final String MDZ_ID = "bsb12345678";

  private final Engine engine;

  private final ExecutorService executorService;

  private final ProcessorAdapter<TestMessage> processorAdapter;

  private final LockManager lockManager;

  private final WorkflowFixture workflowFixture;

  @Autowired
  public LocksAreEffectiveTest(
      Engine engine,
      RedisProperties redisProperties,
      RoutingProperties routingProperties,
      RabbitMQ rabbitMQ,
      ProcessorAdapter<TestMessage> processorAdapter,
      LockManager lockManager) {
    this.engine = engine;
    this.processorAdapter = processorAdapter;
    this.lockManager = lockManager;
    executorService = Executors.newSingleThreadExecutor();
    this.workflowFixture = new WorkflowFixture(rabbitMQ, routingProperties, redisProperties);
  }

  @TestConfiguration
  static class LocksAreEffectiveConfiguration {
    @Bean
    public IncomingMessageType incomingMessageType() {
      return new IncomingMessageType(TestMessage.class);
    }

    @Bean
    public ProcessorAdapter<TestMessage> processorAdapter() {
      return new ProcessorAdapter<>();
    }

    @Bean
    public FlowSpec flowSpec(ProcessorAdapter<TestMessage> processorAdapter) {
      return FlowBuilder.messageProcessor(TestMessage.class).process(processorAdapter).build();
    }
  }

  @BeforeEach
  void startEngine() throws IOException {
    executorService.submit(engine::start);
    workflowFixture.purge();
  }

  @AfterEach
  void stopEngine() throws IOException {
    engine.stop();
    workflowFixture.purge();
  }

  @DisplayName("then these locks block further processing until released")
  @Test
  public void testLocksAreEffective() throws Exception {

    final ConcurrentHashMap<String, Boolean> foundLocked = new ConcurrentHashMap<>();

    Semaphore firstLockHasBeenAcquired = new Semaphore(1);
    firstLockHasBeenAcquired.acquire();

    processorAdapter.setFunction(
        message -> {
          if ("second".equals(message.getId())) {
            try {
              firstLockHasBeenAcquired.acquire();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
          foundLocked.put(message.getId(), lockManager.isLocked(MDZ_ID));
          lockManager.acquire(MDZ_ID); // release happens automatically
          firstLockHasBeenAcquired.release();
          return message;
        });

    workflowFixture.send(new TestMessage("first"), new TestMessage("second"));
    workflowFixture.waitForMessages(2);

    System.out.println(foundLocked);

    assertThat(foundLocked.values()).containsExactly(false, true);
  }

  private static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
