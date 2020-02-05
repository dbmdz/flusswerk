package de.digitalcollections.flusswerk.spring.boot.starter;

import de.digitalcollections.flusswerk.engine.Engine;
import de.digitalcollections.flusswerk.engine.flow.Flow;
import de.digitalcollections.flusswerk.engine.messagebroker.MessageBroker;
import de.digitalcollections.flusswerk.engine.messagebroker.MessageBrokerBuilder;
import de.digitalcollections.flusswerk.engine.model.Message;
import de.digitalcollections.flusswerk.engine.reporting.ProcessReport;
import java.io.IOException;
import org.springframework.beans.factory.ObjectProvider;
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
    FlusswerkProperties.Connection connection = flusswerkProperties.getConnection();
    FlusswerkProperties.Processing processing = flusswerkProperties.getProcessing();
    FlusswerkProperties.Routing routing = flusswerkProperties.getRouting();
    final MessageBrokerBuilder builder = new MessageBrokerBuilder();

    if (isSet(connection.getUsername())) {
      builder.username(connection.getUsername());
    }

    if (isSet(connection.getPassword())) {
      builder.password(connection.getPassword());
    }

    if (isSet(connection.getConnectTo())) {
      builder.connectTo(connection.getConnectTo());
    } else {
      throw new IllegalArgumentException("connect-to is missing");
    }

    if (isSet(connection.getVirtualHost())) {
      builder.virtualHost(connection.getVirtualHost());
    }

    if (isSet(processing.getMaxRetries())) {
      builder.maxRetries(processing.getMaxRetries());
    }

    if (isSet(routing.getExchange())) {
      builder.exchange(routing.getExchange());
    }

    if (isSet(routing.getReadFrom())) {
      builder.readFrom(routing.getReadFrom());
    }
    if (isSet(routing.getWriteTo())) {
      builder.writeTo(routing.getWriteTo());
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
  public <I, M extends Message<I>, R, W> Engine engine(
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
    if (flusswerkProperties.getProcessing().getThreads() != null) {
      threads = flusswerkProperties.getProcessing().getThreads();
    } else {
      threads = 5;
    }

    ProcessReport processReport = processReportProvider.getIfAvailable();
    return new Engine(messageBroker, flow, threads, processReport);
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
