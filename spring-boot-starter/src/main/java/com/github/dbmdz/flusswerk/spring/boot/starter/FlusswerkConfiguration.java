package com.github.dbmdz.flusswerk.spring.boot.starter;

import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.messagebroker.builder.BuildStep;
import com.github.dbmdz.flusswerk.framework.messagebroker.builder.MessageBrokerBuilder;
import com.github.dbmdz.flusswerk.framework.messagebroker.builder.RabbitMQ;
import com.github.dbmdz.flusswerk.framework.messagebroker.builder.SendToStep;
import com.github.dbmdz.flusswerk.framework.messagebroker.builder.ViaStep;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import com.github.dbmdz.flusswerk.spring.boot.starter.monitoring.BaseMetrics;
import com.github.dbmdz.flusswerk.spring.boot.starter.monitoring.MeterFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Spring configuration to provide beans for{@link MessageBroker} and {@link Engine}. */
@Configuration
@Import(FlusswerkPropertiesConfiguration.class)
public class FlusswerkConfiguration {

  @Bean
  @ConditionalOnMissingBean(name="messageImplementation")
  public MessageImplementation<Message> messageImplementation() {
    return new MessageImplementation<>(Message.class);
  }

  /**
   * @param flusswerkProperties The external configuration from <code>application.yml</code>
   * @param messageImplementation A custom {@link Message} implementation to use.
   * @return The message broker for this job.
   */
  @Bean
  public <M extends Message> MessageBroker<M> messageBroker(
      FlusswerkProperties flusswerkProperties, @Qualifier("messageImplementation") MessageImplementation<M> messageImplementation) {

    var connection = flusswerkProperties.getConnection();

    if (connection == null || !isSet(connection.getConnectTo())) {
      throw new IllegalArgumentException("flusswerk.connection.connect-to is missing");
    }

    RabbitMQ rabbitMQ = RabbitMQ.host(connection.getHost(), connection.getPort());

    if (isSet(connection.getUsername()) && isSet(connection.getPassword())) {
      rabbitMQ.auth(connection.getUsername(), connection.getPassword());
    } else {
      throw new RuntimeException("Missing RabbitMQ username or password");
    }

    if (isSet(connection.getVirtualHost())) {
      rabbitMQ.virtualHost(connection.getVirtualHost());
    }

    var routing = flusswerkProperties.getRouting();
    var processing = flusswerkProperties.getProcessing();

    SendToStep<? extends Message> sendToStep = null;
    if (routing != null && isSet(routing.getReadFrom())) {
      sendToStep =
          MessageBrokerBuilder.read(messageImplementation.getMessageClass())
              .from(routing.getReadFrom());

      if (messageImplementation.hasMixin()) {
        sendToStep.usingJacksonMixin(messageImplementation.getMixin());
      }

      if (processing != null && isSet(processing.getMaxRetries())) {
        sendToStep.maxRetries(processing.getMaxRetries());
      }
    }

    if (routing != null && isSet(routing.getExchange())) {
      rabbitMQ.exchange(routing.getExchange());
    }

    ViaStep<M> viaStep;
    if (routing != null && isSet(routing.getWriteTo())) {
      if (sendToStep == null) {
        viaStep = (ViaStep<M>) MessageBrokerBuilder.sendTo(routing.getWriteTo());
      } else {
        viaStep = (ViaStep<M>) sendToStep.sendTo(routing.getWriteTo());
      }
    } else {
      if (sendToStep == null) {
        viaStep = (ViaStep<M>) MessageBrokerBuilder.sendAnywhere();
      } else {
        viaStep = (ViaStep<M>) sendToStep.sendNothing();
      }
    }

    if (connection.getVirtualHost() != null) {
      rabbitMQ.virtualHost(connection.getVirtualHost());
    }

    BuildStep<M> buildStep = viaStep.via(rabbitMQ);

    return buildStep.build();
  }

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
  public <M extends Message, R, W> Engine<M> engine(
      @Qualifier("messageBroker") MessageBroker<M> messageBroker,
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
    return new Engine<>(messageBroker, flow, threads, processReport);
  }

  @Bean
  @ConditionalOnMissingBean(
      name="metrics"
  )
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
