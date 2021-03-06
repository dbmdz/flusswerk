package com.github.dbmdz.flusswerk.framework.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.rabbitmq.client.AMQP.Queue.PurgeOk;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("A Queue")
class QueueTest {

  private RabbitClient rabbitClient;
  private Channel channel;
  private Queue queue;

  @BeforeEach
  void setUp() {
    rabbitClient = mock(RabbitClient.class);
    channel = mock(Channel.class);
    when(rabbitClient.getChannel()).thenReturn(channel);
    queue = new Queue("test.queue", rabbitClient);
  }

  @DisplayName("should purge all messages")
  @Test
  void purge() throws IOException {
    var expected = 123;
    var purgeOk = new PurgeOk.Builder().messageCount(expected).build();
    when(channel.queuePurge(queue.getName())).thenReturn(purgeOk);
    assertThat(queue.purge()).isEqualTo(expected);
  }

  @Test
  void messageCount() throws IOException {
    var expected = 123123L;
    when(channel.messageCount(queue.getName())).thenReturn(expected);

    assertThat(queue.messageCount()).isEqualTo(expected);
  }

  @Test
  void receive() throws IOException, InvalidMessageException {
    var expected = new Message("13123123");
    when(rabbitClient.receive(queue.getName())).thenReturn(expected);
    assertThat(queue.receive()).contains(expected);
  }

  @Test
  void testEquals() {
    var expected = new Queue(queue.getName(), mock(RabbitClient.class));
    assertThat(queue).isEqualTo(expected);
  }

  @Test
  void testHashCode() {
    var expected = new Queue(queue.getName(), mock(RabbitClient.class));
    assertThat(queue.hashCode()).isEqualTo(expected.hashCode());
  }

  @Test
  void testToString() {
    assertThat(queue.toString()).contains(queue.getName());
  }
}
