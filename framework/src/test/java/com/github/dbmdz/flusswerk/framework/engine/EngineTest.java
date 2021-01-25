package com.github.dbmdz.flusswerk.framework.engine;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The Engine")
class EngineTest {

  private Channel channel;
  private List<FlusswerkConsumer> consumers;
  private List<Worker> workers;
  private Engine engine;

  @BeforeEach
  void setUp() {
    channel = mock(Channel.class);
    consumers = List.of(mockConsumer("consumer1", "queue1"), mockConsumer("consumer2", "queue2"));

    workers = List.of(mock(Worker.class), mock(Worker.class));
    engine = new Engine(channel, consumers, workers);
  }

  private FlusswerkConsumer mockConsumer(String consumerTag, String queue) {
    FlusswerkConsumer consumer = mock(FlusswerkConsumer.class);
    when(consumer.getConsumerTag()).thenReturn(consumerTag);
    when(consumer.getInputQueue()).thenReturn(queue);
    return consumer;
  }

  @Test
  public void engineShouldStartAllWorkers() {
    engine.start();
    workers.forEach(worker -> verify(worker).run());
    engine.stop();
  }

  @Test
  public void engineShouldConnectAllConsumers() throws IOException {
    engine.start();
    for (FlusswerkConsumer consumer : consumers) {
      verify(channel).basicConsume(eq(consumer.getInputQueue()), eq(false), eq(consumer));
    }
    engine.stop();
  }

  @Test
  public void engineShouldStopAllWorkers() {
    engine.start();
    engine.stop();
    workers.forEach(worker -> verify(worker).stop());
  }

  @Test
  public void engineShouldDisconnectAllConsumers() throws IOException {
    engine.start();
    engine.stop();
    for (FlusswerkConsumer consumer : consumers) {
      String consumerTag = consumer.getConsumerTag();
      verify(channel).basicCancel(eq(consumerTag));
    }
  }
}
