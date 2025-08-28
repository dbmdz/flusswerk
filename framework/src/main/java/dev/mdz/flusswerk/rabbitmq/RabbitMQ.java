package dev.mdz.flusswerk.rabbitmq;

import dev.mdz.flusswerk.config.properties.RoutingProperties;
import dev.mdz.flusswerk.model.Message;
import dev.mdz.flusswerk.reporting.Tracing;
import java.util.HashMap;
import java.util.Map;

/** Interactions with RabbitMQ - send messages, get the number of messages in a queue and more. */
public class RabbitMQ {

  private final Map<String, Queue> queues;
  private final Map<String, Route> routes;
  private final Map<String, Topic> topics;
  private final RabbitClient rabbitClient;
  private final MessageBroker messageBroker;
  private final Tracing tracing;

  /**
   * Creates a new Queues instance.
   *
   * @param rabbitClient the connection to RabbitMQ.
   */
  public RabbitMQ(
      RoutingProperties routingProperties,
      RabbitClient rabbitClient,
      MessageBroker messageBroker,
      Tracing tracing) {
    this.tracing = tracing;
    // use RabbitConnection to prevent uncontrolled access to Channel from user app
    this.queues = new HashMap<>();
    this.routes = new HashMap<>();
    this.topics = new HashMap<>();
    this.rabbitClient = rabbitClient;
    this.messageBroker = messageBroker;

    routingProperties
        .getIncoming()
        .forEach(
            (queue) -> {
              addQueue(queue);
              var failurePolicy = routingProperties.getFailurePolicy(queue);
              addQueue(failurePolicy.getFailedRoutingKey());
              addQueue(failurePolicy.getRetryRoutingKey());
            });

    routingProperties
        .getOutgoing()
        .forEach(
            (routeName, topicNames) -> {
              Route route = new Route(routeName);
              topicNames.forEach(
                  name -> {
                    addQueue(name);
                    var topic = new Topic(name, messageBroker, tracing);
                    topics.put(name, topic);
                    route.addTopic(topic);
                  });
              routes.put(routeName, route);
            });
  }

  private void addQueue(String name) {
    this.queues.put(name, new Queue(name, rabbitClient));
  }

  /**
   * A route (collection of topics) to send messages to.
   *
   * @param name a route as configured in application.yml
   * @return the corresponding route
   */
  public Route route(String name) {
    return routes.get(name);
  }

  /**
   * A topic to send messages to.
   *
   * @param name The topic's name.
   * @return The corresponding topic.
   */
  public Topic topic(String name) {
    return topics.computeIfAbsent(name, key -> new Topic(name, messageBroker, tracing));
  }

  /**
   * A RabbitMQ queue.
   *
   * @param name The queue's name.
   * @return The corresponding queue.
   */
  public Queue queue(String name) {
    return queues.computeIfAbsent(name, key -> new Queue(key, rabbitClient));
  }

  /**
   * Acknowledges a message with RabbitMQ. This needs the message to have an Envelope with a valid
   * delivery tag (usually because it came from RabbitMQ in the first place).
   *
   * @param message The message to acknowledge.
   */
  public void ack(Message message) {
    rabbitClient.ack(message.getEnvelope());
  }
}
