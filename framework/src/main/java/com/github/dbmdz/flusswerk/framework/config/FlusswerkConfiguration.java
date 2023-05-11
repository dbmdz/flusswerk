package com.github.dbmdz.flusswerk.framework.config;

import com.github.dbmdz.flusswerk.framework.config.properties.AppProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.ProcessingProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.engine.FlusswerkConsumer;
import com.github.dbmdz.flusswerk.framework.engine.Task;
import com.github.dbmdz.flusswerk.framework.engine.Worker;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.jackson.FlusswerkObjectMapper;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.monitoring.FlowMetrics;
import com.github.dbmdz.flusswerk.framework.monitoring.FlusswerkMetrics;
import com.github.dbmdz.flusswerk.framework.monitoring.MeterFactory;
import com.github.dbmdz.flusswerk.framework.rabbitmq.MessageBroker;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitConnection;
import com.github.dbmdz.flusswerk.framework.reporting.DefaultProcessReport;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Spring configuration to provide beans for{@link MessageBroker} and {@link Engine}. */
@Configuration
@Import(FlusswerkPropertiesConfiguration.class)
public class FlusswerkConfiguration {

  @Bean
  public Flow flow(Optional<FlowSpec> flowSpec) {
    // No FlowSpec → no Flow. We will have to handle this case when creating the
    // Engine bean as the sole consumer of the Flow bean.
    return flowSpec.map(Flow::new).orElse(null);
  }

  @Bean
  public Engine engine(
      Optional<Flow> flow,
      List<FlusswerkConsumer> flusswerkConsumers,
      ProcessingProperties processingProperties,
      RabbitConnection rabbitConnection,
      Set<FlowMetrics> flowMetrics,
      MeterFactory meterFactory,
      List<Worker> workers) {

    if (flow.isEmpty()) {
      return null; // No Flow, nothing to do
    }

    flow.get().registerFlowMetrics(flowMetrics);

    return new Engine(rabbitConnection.getChannel(), flusswerkConsumers, workers);
  }

  @Bean
  public FlusswerkObjectMapper flusswerkObjectMapper(
      ObjectProvider<IncomingMessageType> incomingMessageType) {
    return new FlusswerkObjectMapper(incomingMessageType.getIfAvailable(IncomingMessageType::new));
  }

  @Bean
  public PriorityBlockingQueue<Task> taskQueue() {
    return new PriorityBlockingQueue<>();
  }

  @Bean
  public List<Worker> workers(
      AppProperties appProperties,
      Optional<Flow> flow,
      MessageBroker messageBroker,
      ProcessingProperties processingProperties,
      Optional<ProcessReport> processReport,
      PriorityBlockingQueue<Task> taskQueue,
      Tracing tracing,
      FlusswerkMetrics metrics) {
    return flow.map(
            theFlow ->
                IntStream.range(0, processingProperties.threads())
                    .mapToObj(
                        n ->
                            new Worker(
                                theFlow,
                                metrics,
                                messageBroker,
                                processReport.orElseGet(
                                    () -> new DefaultProcessReport(appProperties.name())),
                                taskQueue,
                                tracing))
                    .collect(
                        Collectors.toList())) // Return workers for each thread to process the Flow
        .orElse(Collections.emptyList()); // No Flow, nothing to do
  }

  @Bean
  public List<FlusswerkConsumer> flusswerkConsumers(
      FlusswerkObjectMapper flusswerkObjectMapper,
      ProcessingProperties processingProperties,
      RabbitConnection rabbitConnection,
      RoutingProperties routingProperties,
      PriorityBlockingQueue<Task> taskQueue) {
    int maxPriority = routingProperties.getIncoming().size();
    List<FlusswerkConsumer> flusswerkConsumers = new ArrayList<>();

    Semaphore availableWorkers = new Semaphore(processingProperties.threads());
    for (int i = 0; i < routingProperties.getIncoming().size(); i++) {
      String queueName = routingProperties.getIncoming().get(i);
      int priority = maxPriority - i;
      for (int k = 0; k < processingProperties.threads(); k++) {
        flusswerkConsumers.add(
            new FlusswerkConsumer(
                availableWorkers,
                rabbitConnection.getChannel(),
                flusswerkObjectMapper,
                queueName,
                priority,
                taskQueue));
      }
    }
    return Collections.unmodifiableList(flusswerkConsumers);
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
