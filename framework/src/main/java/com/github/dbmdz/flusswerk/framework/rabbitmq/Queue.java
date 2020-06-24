package com.github.dbmdz.flusswerk.framework.rabbitmq;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Queue {

  private static final Logger LOGGER = LoggerFactory.getLogger(Queue.class);


  private final String name;
  private final Channel channel;

  Queue(String name, Channel channel) {
    this.name = name;
    this.channel = channel;
  }

  /**
   * Removes all messages from a queue. These messages cannot be restored.
   *
   * @return the number of deleted messages
   * @throws IOException if an error occurs while purging
   */
  public int purge() throws IOException {
    var purgeOk = channel.queuePurge(this.name);
    var deletedMessages = purgeOk.getMessageCount();
    LOGGER.warn("Purged queue {} ({} messages deleted)", this.name, deletedMessages);
    return deletedMessages;
  }

  /**
   *
   * @return the number of messages in this queue.
   * @throws IOException if communication with RabbitMQ fails.
   */
  public long messageCount() throws IOException {
    return channel.messageCount(this.name);
  }

}
