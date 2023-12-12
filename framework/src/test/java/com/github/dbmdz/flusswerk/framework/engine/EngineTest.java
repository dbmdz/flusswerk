package com.github.dbmdz.flusswerk.framework.engine;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitClient;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The Engine")
class EngineTest {

  private RabbitClient rabbitClient;
  private List<FlusswerkConsumer> consumers;
  private List<Worker> workers;
  private Engine engine;

  @BeforeEach
  void setUp() {
    rabbitClient = mock(RabbitClient.class);
    consumers = List.of(mockConsumer("consumer1", "queue1"), mockConsumer("consumer2", "queue2"));

    workers = List.of(mock(Worker.class), mock(Worker.class));
    engine = new Engine(rabbitClient, consumers, workers, new TestingExecutorService());
  }

  private FlusswerkConsumer mockConsumer(String consumerTag, String queue) {
    FlusswerkConsumer consumer = mock(FlusswerkConsumer.class);
    when(consumer.getConsumerTag()).thenReturn(consumerTag);
    when(consumer.getInputQueue()).thenReturn(queue);
    return consumer;
  }

  @DisplayName("should start all workers")
  @Test
  public void engineShouldStartAllWorkers() {
    engine.start();
    workers.forEach(worker -> verify(worker).run());
    engine.stop();
  }

  @DisplayName("should connect all consumers")
  @Test
  public void engineShouldConnectAllConsumers() {
    engine.start();
    for (FlusswerkConsumer consumer : consumers) {
      verify(rabbitClient).consume(eq(consumer), eq(false));
    }
    engine.stop();
  }

  @DisplayName("should stop all workers")
  @Test
  public void engineShouldStopAllWorkers() {
    engine.start();
    engine.stop();
    workers.forEach(worker -> verify(worker).stop());
  }

  @DisplayName("should disconnect all consumers")
  @Test
  public void engineShouldDisconnectAllConsumers() throws IOException {
    engine.start();
    engine.stop();
    for (FlusswerkConsumer consumer : consumers) {
      String consumerTag = consumer.getConsumerTag();
      verify(rabbitClient).cancel(eq(consumerTag));
    }
  }
}
