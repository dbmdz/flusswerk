package com.github.dbmdz.flusswerk.integration;

import com.github.dbmdz.flusswerk.framework.rabbitmq.Queue;
import java.io.IOException;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class RabbitUtilAssert extends AbstractAssert<RabbitUtilAssert, RabbitUtil> {

  protected RabbitUtilAssert(RabbitUtil rabbitUtil) {
    super(rabbitUtil, RabbitUtilAssert.class);
  }

  public static RabbitUtilAssert assertThat(RabbitUtil rabbitUtil) {
    return new RabbitUtilAssert(rabbitUtil);
  }

  public RabbitUtilAssert allQueuesAreEmpty() {
    isNotNull();
    actual
        .allQueues()
        .forEach(
            queue ->
                Assertions.assertThat(getMessageCount(queue))
                    .as("Queue " + queue.getName() + " is not empty")
                    .isZero());
    return this;
  }

  private static long getMessageCount(Queue queue) {
    try {
      return queue.messageCount();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
