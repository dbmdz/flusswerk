package dev.mdz.flusswerk.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.AMQP.Queue.PurgeOk;
import dev.mdz.flusswerk.exceptions.InvalidMessageException;
import dev.mdz.flusswerk.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("A Queue")
class QueueTest {

  private RabbitClient rabbitClient;
  private Queue queue;

  @BeforeEach
  void setUp() {
    rabbitClient = mock(RabbitClient.class);
    queue = new Queue("test.queue", rabbitClient);
  }

  @DisplayName("should purge all messages")
  @Test
  void purge() {
    var expected = 123;
    var purgeOk = new PurgeOk.Builder().messageCount(expected).build();
    when(rabbitClient.queuePurge(queue.getName())).thenReturn(purgeOk);
    assertThat(queue.purge()).isEqualTo(expected);
  }

  @Test
  void messageCount() {
    var expected = 123123L;
    when(rabbitClient.getMessageCount(queue.getName())).thenReturn(expected);

    assertThat(queue.messageCount()).isEqualTo(expected);
  }

  @Test
  void receive() throws InvalidMessageException {
    var expected = new Message();
    when(rabbitClient.receive(queue.getName(), false)).thenReturn(expected);
    assertThat(queue.receive(false)).contains(expected);
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
