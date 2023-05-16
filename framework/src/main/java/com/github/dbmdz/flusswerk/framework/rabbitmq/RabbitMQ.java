package com.github.dbmdz.flusswerk.framework.rabbitmq;

import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Interactions with RabbitMQ - send messages, get the number of messages in a queue and more. */
@Component
public class RabbitMQ {

  private final Map<String, Queue> queues;
  private final Map<String, Topic> routes;
  private final Map<String, Topic> topics;

  private final Channel channel;
  private final RabbitClient rabbitClient;
  private final Tracing tracing;
  private final RoutingProperties routingProperties;
  private final MessageBroker messageBroker;

  /**
   * Creates a new Queues instance.
   *
   * @param rabbitClient the connection to RabbitMQ.
   */
  public RabbitMQ(
      RoutingProperties routingProperties,
      RabbitClient rabbitClient,
      Tracing tracing,
      MessageBroker messageBroker) {
    this.tracing = tracing;
    // use RabbitConnection to prevent uncontrolled access to Channel from user app
    this.queues = new HashMap<>();
    this.routes = new HashMap<>();
    this.topics = new HashMap<>();
    this.channel = rabbitClient.getChannel();
    this.rabbitClient = rabbitClient;
    this.routingProperties = routingProperties;
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
            (route, topicName) -> {
              addQueue(topicName);
              var topic =
                  new Topic(
                      topicName, routingProperties.getExchange(topicName), rabbitClient, tracing);
              topics.put(topicName, topic);
              routes.put(route, topic);
            });
  }

  private void addQueue(String name) {
    this.queues.put(name, new Queue(name, rabbitClient));
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
    return topics.computeIfAbsent(
        name, key -> new Topic(name, routingProperties.getExchange(name), rabbitClient, tracing));
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
   * Acknowledges a message with RabbitMQ. This needs the message to have an Envelope with an valid
   * delivery tag (usually because it came from RabbitMQ in the first place).
   *
   * @param message The message to acknowledge.
   * @throws IOException If communication with RabbitMQ fails.
   */
  public void ack(Message message) throws IOException {
    var deliveryTag = message.getEnvelope().getDeliveryTag();
    channel.basicAck(deliveryTag, false);
  }

  public void stop(Message message) throws IOException {
    messageBroker.fail(message);
  }

  public boolean retry(Message message) throws IOException {
    return messageBroker.reject(message);
  }
}
