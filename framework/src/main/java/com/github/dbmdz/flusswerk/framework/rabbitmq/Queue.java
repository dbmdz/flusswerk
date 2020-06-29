package com.github.dbmdz.flusswerk.framework.rabbitmq;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Queue {

  private static final Logger LOGGER = LoggerFactory.getLogger(Queue.class);

  private final String name;
  private final Channel channel;
  private final RabbitClient rabbitClient;

  Queue(String name, RabbitClient rabbitClient) {
    this.name = requireNonNull(name);
    this.channel = rabbitClient.getChannel();
    this.rabbitClient = rabbitClient;
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
   * @return the number of messages in this queue.
   * @throws IOException if communication with RabbitMQ fails.
   */
  public long messageCount() throws IOException {
    return channel.messageCount(this.name);
  }

  public Optional<Message> receive() throws IOException, InvalidMessageException {
    Message message = rabbitClient.receive(name);
    return Optional.ofNullable(message);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Queue) {
      Queue queue = (Queue) o;
      return name.equals(queue.name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return "Queue{name='" + name + "'}";
  }

  public String getName() {
    return name;
  }
}
