package com.github.dbmdz.flusswerk.integration;

import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import java.io.IOException;

public class WorkflowFixture {

  private final RabbitMQ rabbitMQ;
  private final RoutingProperties routing;
  private final RabbitUtil rabbitUtil;

  public WorkflowFixture(RabbitMQ rabbitMQ, RoutingProperties routing) {
    this.rabbitMQ = rabbitMQ;
    this.routing = routing;
    this.rabbitUtil = new RabbitUtil(rabbitMQ, routing);
  }

  public void purge() throws IOException {
    rabbitUtil.purgeQueues();
  }

  public void send(Message... messages) throws IOException {
    rabbitMQ.topic(routing.getIncoming().get(0)).send(messages);
  }

  public void waitForMessages(int n)
      throws InvalidMessageException, IOException, InterruptedException {
    int messagesReceived = 0;
    String inputQueue = routing.getIncoming().get(0);
    String outputQueue = routing.getOutgoing().get("default");
    while (messagesReceived < n) {
      rabbitUtil.waitAndAck(outputQueue, routing.getFailurePolicy(inputQueue).getBackoff());
      messagesReceived++;
    }
  }
}