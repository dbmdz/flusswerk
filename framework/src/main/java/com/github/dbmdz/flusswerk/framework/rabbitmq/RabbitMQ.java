package com.github.dbmdz.flusswerk.framework.rabbitmq;

import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import java.util.HashMap;
import java.util.Map;

/**
 * Interactions with RabbitMQ - send messages, get the number of messages in a queue and more.
 */
public class RabbitMQ {

  private final Map<String, Queue> queues;
  private final Map<String, Topic> routes;
  private final Map<String, Topic> topics;

  /**
   * Creates a new Queues instance.
   *
   * @param rabbitConnection the connection to RabbitMQ.
   */
  public RabbitMQ(RoutingProperties routingProperties, RabbitConnection rabbitConnection, MessageBroker messageBroker) {
    // use RabbitConnection to prevent uncontrolled access to Channel from user app
    this.queues = new HashMap<>();
    this.routes = new HashMap<>();
    this.topics = new HashMap<>();

    routingProperties.getIncoming().forEach(
        (queue) -> {
          addQueue(queue, rabbitConnection);
          var failurePolicy = routingProperties.getFailurePolicy(queue);
          addQueue(failurePolicy.getFailedRoutingKey(), rabbitConnection);
          addQueue(failurePolicy.getRetryRoutingKey(), rabbitConnection);
        }
    );

    routingProperties.getOutgoing().forEach(
        (route, topicName) -> {
          addQueue(topicName, rabbitConnection);
          var topic = new Topic(topicName, messageBroker);
          topics.put(topicName, topic);
          routes.put(route, topic);
        }
    );
  }

  private void addQueue(String name, RabbitConnection rabbitConnection) {
    this.queues.put(name, new Queue(name, rabbitConnection.getChannel()));
  }

  /**
   * A topic to send messages to.
   *
   * @param name a route as configured in application.yml
   * @return the corresponding topic
   */
  public Topic route(String name) {
    return routes.get(name);
  }

  /**
   * A topic to send messages to.
   *
   * @param name The topic's name.
   * @return The corresponding topic.
   */
  public Topic topic(String name) {
    return topics.get(name);
  }

  /**
   * A RabbitMQ queue.
   *
   * @param name The queue's name.
   * @return The corresponding queue.
   */
  public Queue queue(String name) {
    return queues.get(name);
  }

}
