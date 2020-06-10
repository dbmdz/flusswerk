package com.github.dbmdz.flusswerk.framework.config;

import com.github.dbmdz.flusswerk.framework.config.properties.FlusswerkProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.locking.LockManager;
import com.github.dbmdz.flusswerk.framework.locking.NoOpLockManager;
import com.github.dbmdz.flusswerk.framework.locking.RedisLockManager;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.messagebroker.RabbitClient;
import com.github.dbmdz.flusswerk.framework.messagebroker.RabbitConnection;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import com.github.dbmdz.flusswerk.framework.spring.MessageImplementation;
import com.github.dbmdz.flusswerk.framework.spring.monitoring.BaseMetrics;
import com.github.dbmdz.flusswerk.framework.spring.monitoring.MeterFactory;
import java.io.IOException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Spring configuration to provide beans for{@link MessageBroker} and {@link Engine}. */
@Configuration
@Import(FlusswerkPropertiesConfiguration.class)
public class FlusswerkConfiguration {

  /**
   * @param messageBroker The messageBroker to use.
   * @param flowProvider The flow to use (optional).
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
      ObjectProvider<Flow<M, R, W>> flowProvider,
      FlusswerkProperties flusswerkProperties,
      ObjectProvider<ProcessReport> processReportProvider) {
    Flow<M, R, W> flow = flowProvider.getIfAvailable();
    if (flow == null) {
      throw new RuntimeException("Missing flow definition. Please create a Flow bean.");
    }

    int threads;
    var processing = flusswerkProperties.getProcessing();
    if (processing != null && processing.getThreads() != null) {
      threads = flusswerkProperties.getProcessing().getThreads();
    } else {
      threads = 5;
    }

    ProcessReport processReport = processReportProvider.getIfAvailable();
    return new Engine(name, messageBroker, flow, threads, processReport);
  }

  @Bean
  @ConditionalOnMissingBean
  public BaseMetrics metrics(MeterFactory meterFactory) {
    return new BaseMetrics(meterFactory);
  }

  @Bean
  public MessageBroker messageBroker(
      ObjectProvider<MessageImplementation> messageImplementation,
      FlusswerkProperties flusswerkProperties)
      throws IOException {
    RabbitConnection rabbitConnection = new RabbitConnection(flusswerkProperties.getConnection());
    RabbitClient client =
        new RabbitClient(
            messageImplementation.getIfAvailable(MessageImplementation::new), rabbitConnection);
    return new MessageBroker(flusswerkProperties.getRouting(), client);
  }

  @Bean
  public LockManager lockManager(FlusswerkProperties flusswerkProperties) {
    if (flusswerkProperties.getRedis() == null) {
      return new NoOpLockManager();
    } else {
      return new RedisLockManager();
    }
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
