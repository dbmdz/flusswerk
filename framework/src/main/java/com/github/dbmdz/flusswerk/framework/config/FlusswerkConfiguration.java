package com.github.dbmdz.flusswerk.framework.config;

import com.github.dbmdz.flusswerk.framework.config.properties.FlusswerkProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RedisProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.locking.LockManager;
import com.github.dbmdz.flusswerk.framework.locking.NoOpLockManager;
import com.github.dbmdz.flusswerk.framework.locking.RedisLockManager;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.monitoring.BaseMetrics;
import com.github.dbmdz.flusswerk.framework.monitoring.FlowMetrics;
import com.github.dbmdz.flusswerk.framework.monitoring.MeterFactory;
import com.github.dbmdz.flusswerk.framework.rabbitmq.MessageBroker;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitClient;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitConnection;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import com.github.dbmdz.flusswerk.framework.reporting.DefaultProcessReport;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Spring configuration to provide beans for{@link MessageBroker} and {@link Engine}. */
@Configuration
@Import(FlusswerkPropertiesConfiguration.class)
public class FlusswerkConfiguration {

  @Bean
  public <M extends Message, R, W> Flow<M, R, W> flow(
      ObjectProvider<FlowSpec<M, R, W>> flowSpec, LockManager lockManager) {
    var spec = flowSpec.getIfAvailable();
    if (spec == null) {
      throw new RuntimeException("Missing flow definition. Please create a FlowSpec bean.");
    }
    return new Flow<>(spec, lockManager);
  }

  /**
   * @param messageBroker The messageBroker to use.
   * @param flow The flow to use (optional).
   * @param flusswerkProperties The external configuration from <code>application.yml</code>.
   * @param processReportProvider A custom process report provider (optional).
   * @param <M> The used {@link Message} type
   * @param <R> The type of the reader implementation
   * @param <W> The type of the writer implementation
   * @return The {@link Engine} used for this job.
   */
  @Bean
  public <M extends Message, R, W> Engine engine(
      @Value("spring.application.name") String name,
      MessageBroker messageBroker,
      Flow<M, R, W> flow,
      FlusswerkProperties flusswerkProperties,
      ObjectProvider<ProcessReport> processReportProvider,
      Set<FlowMetrics> flowMetrics) {
    flow.registerFlowMetrics(flowMetrics);

    int threads;
    var processing = flusswerkProperties.getProcessing();
    if (processing != null && processing.getThreads() != null) {
      threads = flusswerkProperties.getProcessing().getThreads();
    } else {
      threads = 5;
    }

    ProcessReport processReport =
        processReportProvider.getIfAvailable(() -> new DefaultProcessReport(name));
    return new Engine(messageBroker, flow, threads, processReport);
  }

  @Bean
  public BaseMetrics baseMetrics(MeterFactory meterFactory) {
    return new BaseMetrics(meterFactory);
  }

  @Bean
  public MeterFactory meterFactory(
      FlusswerkProperties flusswerkProperties,
      @Value("spring.application.name") String name,
      MeterRegistry meterRegistry) {
    return new MeterFactory(flusswerkProperties, name, meterRegistry);
  }

  @Bean
  public RabbitConnection rabbitConnection(FlusswerkProperties flusswerkProperties)
      throws IOException {
    return new RabbitConnection(flusswerkProperties.getRabbitMQ());
  }

  @Bean
  public RabbitClient rabbitClient(
      ObjectProvider<IncomingMessageType> incomingMessageType, RabbitConnection rabbitConnection) {
    return new RabbitClient(
        incomingMessageType.getIfAvailable(IncomingMessageType::new), rabbitConnection);
  }

  @Bean
  public RabbitMQ rabbitMQ(
      FlusswerkProperties flusswerkProperties,
      RabbitClient rabbitClient,
      MessageBroker messageBroker) {
    return new RabbitMQ(flusswerkProperties.getRouting(), rabbitClient, messageBroker);
  }

  @Bean
  public MessageBroker messageBroker(
      FlusswerkProperties flusswerkProperties, RabbitClient rabbitClient) throws IOException {
    return new MessageBroker(flusswerkProperties.getRouting(), rabbitClient);
  }

  @Bean
  public LockManager lockManager(FlusswerkProperties flusswerkProperties) {
    Optional<RedisProperties> redis = flusswerkProperties.getRedis();
    if (redis.isPresent()) {
      Config config = createRedisConfig(redis.get());
      RedissonClient client = Redisson.create(config);
      return new RedisLockManager(client);
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
