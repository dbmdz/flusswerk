package com.github.dbmdz.flusswerk.spring.boot.starter;

import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBrokerBuilderOld;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import com.github.dbmdz.flusswerk.spring.boot.starter.monitoring.BaseMetrics;
import com.github.dbmdz.flusswerk.spring.boot.starter.monitoring.MeterFactory;
import java.io.IOException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Spring configuration to provide beans for{@link MessageBroker} and {@link Engine}. */
@Configuration
@Import(FlusswerkPropertiesConfiguration.class)
public class FlusswerkConfiguration {

  /**
   * @param flusswerkProperties The external configuration from <code>application.yml</code>
   * @param messageImplementation A custom {@link Message} implementation to use.
   * @return The message broker for this job.
   */
  @Bean
  public MessageBroker messageBroker(
      FlusswerkProperties flusswerkProperties,
      ObjectProvider<MessageImplementation> messageImplementation) {

    var connection = flusswerkProperties.getConnection();
    MessageBrokerBuilderOld builder = new MessageBrokerBuilderOld();

    if (connection == null || !isSet(connection.getConnectTo())) {
      throw new IllegalArgumentException("flusswerk.connection.connect-to is missing");
    }

    builder.connectTo(connection.getConnectTo());

    if (isSet(connection.getUsername())) {
      builder.username(connection.getUsername());
    }

    if (isSet(connection.getPassword())) {
      builder.password(connection.getPassword());
    }

    if (isSet(connection.getVirtualHost())) {
      builder.virtualHost(connection.getVirtualHost());
    }

    var processing = flusswerkProperties.getProcessing();
    if (processing != null && isSet(processing.getMaxRetries())) {
      builder.maxRetries(processing.getMaxRetries());
    }

    var routing = flusswerkProperties.getRouting();
    if (routing != null && isSet(routing.getExchange())) {
      builder.exchange(routing.getExchange());
    }

    if (routing != null && isSet(routing.getReadFrom())) {
      builder.readFrom(routing.getReadFrom());
    }

    if (routing != null && isSet(routing.getWriteTo())) {
      builder.writeTo(routing.getWriteTo());
    }

    if (connection.getVirtualHost() != null) {
      builder.virtualHost(connection.getVirtualHost());
    }

    messageImplementation.ifAvailable(
        impl -> {
          if (impl.hasMixin()) {
            builder.useMessageClass(impl.getMessageClass(), impl.getMixin());
          } else {
            builder.useMessageClass(impl.getMessageClass());
          }
        });

    return builder.build();
  }

  /**
   * @param messageBroker The messageBroker to use.
   * @param flowProvider The flow to use (optional).
   * @param flusswerkProperties The external configuration from <code>application.yml</code>.
   * @param processReportProvider A custom process report provider (optional).
   * @param <I> The message's identifier type.
   * @param <M> The used {@link Message} type
   * @param <R> The type of the reader implementation
   * @param <W> The type of the writer implementation
   * @return The {@link Engine} used for this job.
   * @throws IOException If connection to RabbitMQ fails permanently.
   */
  @Bean
  public <I, M extends Message, R, W> Engine engine(
      MessageBroker messageBroker,
      ObjectProvider<Flow<M, R, W>> flowProvider,
      FlusswerkProperties flusswerkProperties,
      ObjectProvider<ProcessReport> processReportProvider)
      throws IOException {
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
    return new Engine(messageBroker, flow, threads, processReport);
  }

  @Bean
  @ConditionalOnMissingBean
  public BaseMetrics metrics(MeterFactory meterFactory) {
    return new BaseMetrics(meterFactory);
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
