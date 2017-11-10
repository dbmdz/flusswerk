package org.mdz.dzp.workflow.neo.engine;

import java.util.function.Function;
import org.mdz.dzp.workflow.neo.engine.model.Job;
import org.mdz.dzp.workflow.neo.engine.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class Flow<R, W> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Flow.class);

  private Function<Message, R> reader;

  private Function<R, W> transformer;

  private Function<W, Message> writer;

//  private final ExecutorService readExecutorService;
//  private final ExecutorService transformExecutorService;
//  private final ExecutorService writeExecutorService;

  private boolean configured;

//  public Engine(
//      @Value("messaging.maxConcurrentReads") int maxConcurrentReads,
//      @Value("messaging.maxConcurrentWrites") int maxConcurrentWrites,
//      @Value("messaging.maxConcurrentTransforms") int maxConcurrentTransforms) {
//
//    this.readExecutorService = Executors.newFixedThreadPool(maxConcurrentReads);
//    this.transformExecutorService = Executors.newFixedThreadPool(maxConcurrentWrites);
//    this.writeExecutorService = Executors.newFixedThreadPool(maxConcurrentTransforms);
//
//    this.reader = null;
//    this.transformer = null;
//    this.writer = null;
//  }

  public Flow() {
    this.reader = null;
    this.transformer = null;
    this.writer = null;
    this.configured = false;
  }

  public Flow<R, W> reader(Function<Message, R> reader) {
    this.reader = reader;
    return this;
  }

  public Flow<R, W> transformer(Function<R, W> transformer) {
    this.transformer = transformer;
    return this;
  }


  public Flow<R, W> writer(Function<W, Message> writer) {
    this.writer = writer;
    return this;
  }

  public void start() {
    if (reader == null || transformer == null || writer == null) {
      throw new IllegalStateException("Need to set reader, transformer and writer");
    }
    configured = true;
  }

  @RabbitListener(bindings = @QueueBinding(
      value = @Queue(value = "${messaging.inboundQueue}", durable = "true"),
      exchange = @Exchange(value = "${messaging.exchange}", ignoreDeclarationExceptions = "true"),
      key = "orderRoutingKey"))
  public Message process(Message message) {
    while (!configured) {
      LOGGER.debug("Waiting until engine is configured");
    }

    Job job = new Job<>(message, reader, transformer, writer);
//    try {
//      readExecutorService.submit(job::read).get();
//      transformExecutorService.submit(job::transform);
//      writeExecutorService.submit(job::write);
    job.read();
    job.transform();
    job.write();
    return job.getResult();
//    } catch (InterruptedException | ExecutionException e) {
//      throw new AmqpRejectAndDontRequeueException(e);
//    }
  }

}
