package org.mdz.dzp.workflow.neo.engine;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AmqpSetup {

  private static final boolean DURABLE = true;

  private static final boolean NO_AUTO_DELETE = false;

  private static final boolean NOT_EXCLUSIVE = false;

  private final QueueConfig inbound;

  private final QueueConfig inboundDlx;

  private final QueueConfig outbound;

  private final QueueConfig failed;

  private final String messageExchangeName;

  private final String deadLetterExchangeName;

  private final AmqpAdmin amqpAdmin;

  private int retryAfterSeconds;

  class QueueConfig {
    public final String queueName;
    public final String routingKey;

    public QueueConfig(String queueName) {
      this.queueName = queueName;
      this.routingKey = queueName;
    }

  }

  @Autowired
  public AmqpSetup(
      @Value("${messaging.inboundQueue:}") String inboundQueueName,
      @Value("${messaging.outboundQueue:}") String outboundQueueName,
      @Value("${messaging.messageExchange:}") String messageExchangeName,
      @Value("${messaging.deadLetterExchange:}") String deadLetterExchangeName,
      AmqpAdmin amqpAdmin) {
    this.inbound = new QueueConfig(inboundQueueName);
    this.inboundDlx = new QueueConfig("dlx." + inboundQueueName);
    this.failed = new QueueConfig(inboundQueueName + ".failed");
    this.outbound = new QueueConfig(outboundQueueName);
    this.messageExchangeName = messageExchangeName;
    this.deadLetterExchangeName = deadLetterExchangeName;
    this.amqpAdmin = amqpAdmin;
  }

  @PostConstruct
  public void setup() {
    TopicExchange exchange = declareTopicExchange(messageExchangeName);
    TopicExchange deadLetterExchange = declareTopicExchange(deadLetterExchangeName);

    Queue inboundQueue = declareQueue(inbound.queueName, queueConfig());
    declareBinding(inboundQueue, exchange, inbound.routingKey);

    Queue dlxQueue = declareQueue(inboundDlx.queueName, dlxQueueConfig());
    declareBinding(dlxQueue, deadLetterExchange, inbound.routingKey);

    Queue failedQueue = declareQueue(failed.queueName, queueConfig());
    declareBinding(inboundQueue, exchange, failed.routingKey);

    if (!outbound.queueName.isEmpty()) {
      Queue outboundQueue = declareQueue(outbound.queueName, queueConfig());
      declareBinding(outboundQueue, exchange, outbound.queueName);
    }
  }

  private Queue declareQueue(String name, Map<String, Object> config) {
    Queue queue = new Queue(name, DURABLE, NOT_EXCLUSIVE, NO_AUTO_DELETE, config);
    amqpAdmin.declareQueue(queue);
    return queue;
  }

  private Map<String, Object> dlxQueueConfig() {
    Map<String, Object> dlxConfig = new HashMap<>();
    dlxConfig.put("x-dead-letter-exchange", deadLetterExchangeName); // Where to be re-published later
    dlxConfig.put("x-message-ttl", 1000 * retryAfterSeconds);
    return dlxConfig;
  }

  private Map<String, Object> queueConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put("x-dead-letter-exchange", messageExchangeName);
    return config;
  }

  private TopicExchange declareTopicExchange(String name) {
    TopicExchange exchange = new TopicExchange(name, DURABLE, NO_AUTO_DELETE);
    amqpAdmin.declareExchange(exchange);
    return exchange;
  }

  private void declareBinding(Queue queue, TopicExchange exchange, String routingKey) {
    Binding binding = BindingBuilder.bind(queue)
        .to(exchange)
        .with(routingKey);
    amqpAdmin.declareBinding(binding);
  }

}
