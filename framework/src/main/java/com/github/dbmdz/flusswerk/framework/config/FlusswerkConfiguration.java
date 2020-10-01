package com.github.dbmdz.flusswerk.framework.config;

import com.github.dbmdz.flusswerk.framework.config.properties.AppProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.MonitoringProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.ProcessingProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RabbitMQProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RedisProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.DefaultEngine;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.engine.NoOpEngine;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.jackson.FlusswerkObjectMapper;
import com.github.dbmdz.flusswerk.framework.locking.LockManager;
import com.github.dbmdz.flusswerk.framework.locking.NoOpLockManager;
import com.github.dbmdz.flusswerk.framework.locking.RedisLockManager;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.monitoring.DefaultFlowMetrics;
import com.github.dbmdz.flusswerk.framework.monitoring.FlowMetrics;
import com.github.dbmdz.flusswerk.framework.monitoring.MeterFactory;
import com.github.dbmdz.flusswerk.framework.rabbitmq.MessageBroker;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitClient;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitConnection;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import com.github.dbmdz.flusswerk.framework.reporting.DefaultProcessReport;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Spring configuration to provide beans for{@link MessageBroker} and {@link DefaultEngine}. */
@Configuration
@Import(FlusswerkPropertiesConfiguration.class)
public class FlusswerkConfiguration {

  @Bean
  public Tracing tracing() {
    return new Tracing();
  }

  @Bean
  public Flow flow(Optional<FlowSpec> flowSpec, LockManager lockManager, Tracing tracing) {
    if (flowSpec.isEmpty()) {
      return null; // No FlowSpec → no Flow. We will have to handle this case when creating the
      // Engine bean as the sole consumer of the Flow bean.
    }
    return new Flow(flowSpec.get(), lockManager, tracing);
  }

  /**
   * @param messageBroker The messageBroker to use.
   * @param flow The flow to use (optional).
   * @param processingProperties The external configuration from <code>application.yml</code>.
   * @param processReport A custom process report provider (optional).
   * @param flowMetrics The metrics collector.
   * @return The {@link DefaultEngine} used for this job.
   */
  @Bean
  public Engine engine(
      AppProperties appProperties,
      MessageBroker messageBroker,
      Optional<Flow> flow,
      ProcessingProperties processingProperties,
      Optional<ProcessReport> processReport,
      Set<FlowMetrics> flowMetrics,
      Tracing tracing,
      MeterFactory meterFactory) {

    if (flow.isEmpty()) {
      return new NoOpEngine(); // No Flow, nothing to do
    }

    flow.get().registerFlowMetrics(flowMetrics);

    ProcessReport actualProcessReport =
        processReport.orElseGet(() -> new DefaultProcessReport(appProperties.getName()));

    var threads = processingProperties.getThreads();

    return new DefaultEngine(messageBroker, flow.get(), threads, actualProcessReport, tracing);
  }

  @Bean
  public MeterFactory meterFactory(
      AppProperties appProperties,
      MonitoringProperties monitoringProperties,
      MeterRegistry meterRegistry) {
    return new MeterFactory(
        monitoringProperties.getPrefix(), appProperties.getName(), meterRegistry);
  }

  @Bean
  public FlusswerkObjectMapper flusswerkObjectMapper(
      ObjectProvider<IncomingMessageType> incomingMessageType) {
    return new FlusswerkObjectMapper(incomingMessageType.getIfAvailable(IncomingMessageType::new));
  }

  @Bean
  public RabbitConnection rabbitConnection(RabbitMQProperties rabbitMQProperties)
      throws IOException {
    return new RabbitConnection(rabbitMQProperties);
  }

  @Bean
  public RabbitClient rabbitClient(
      FlusswerkObjectMapper flusswerkObjectMapper, RabbitConnection rabbitConnection) {
    return new RabbitClient(flusswerkObjectMapper, rabbitConnection);
  }

  @Bean
  public RabbitMQ rabbitMQ(
      RoutingProperties routingProperties,
      RabbitClient rabbitClient,
      MessageBroker messageBroker,
      Tracing tracing) {
    return new RabbitMQ(routingProperties, rabbitClient, messageBroker, tracing);
  }

  @Bean
  public MessageBroker messageBroker(RoutingProperties routingProperties, RabbitClient rabbitClient)
      throws IOException {
    return new MessageBroker(routingProperties, rabbitClient);
  }

  @Bean
  public LockManager lockManager(RedisProperties redisProperties) {
    if (redisProperties.redisIsAvailable()) {
      Config config = createRedisConfig(redisProperties);
      RedissonClient client = Redisson.create(config);
      return new RedisLockManager(
          client, redisProperties.getKeyspace(), redisProperties.getLockWaitTimeout());
    } else {
      return new NoOpLockManager();
    }
  }

  private Config createRedisConfig(RedisProperties redis) {
    Config config = new Config();
    config.useSingleServer().setAddress(redis.getAddress()).setPassword(redis.getPassword());
    return config;
  }

  public static boolean isSet(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof String) {
      return !((String) value).matches("\\s*");
    }
    return true;
  }
}
