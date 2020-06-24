package com.github.dbmdz.flusswerk.framework.rabbitmq;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Methods for queue management. */
public class Queues {

  private static final Logger LOGGER = LoggerFactory.getLogger(Queues.class);

  private final Channel channel;

  /**
   * Creates a new Queues instance.
   *
   * @param rabbitConnection the connection to RabbitMQ.
   */
  public Queues(RabbitConnection rabbitConnection) {
    // use RabbitConnection to prevent access to channel to prevent
    // uncontrolled access to channel from user app
    this.channel = rabbitConnection.getChannel();
  }

  /**
   * Removes all messages from a queue. These messages cannot be restored.
   *
   * @param queue the queue to purge
   * @return the number of deleted messages
   * @throws IOException if an error occurs while purging
   */
  public int purge(String queue) throws IOException {
    var purgeOk = channel.queuePurge(queue);
    var deletedMessages = purgeOk.getMessageCount();
    LOGGER.warn("Purged queue {} ({} messages deleted)", queue, deletedMessages);
    return deletedMessages;
  }

  public long messageCount(String queue) throws IOException {
    return channel.messageCount(queue);
  }
}
