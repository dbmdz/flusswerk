package com.github.dbmdz.flusswerk.framework.engine;

import com.github.dbmdz.flusswerk.framework.exceptions.RetryProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.SkipProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.model.Envelope;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.monitoring.FlusswerkMetrics;
import com.github.dbmdz.flusswerk.framework.rabbitmq.MessageBroker;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.util.Collection;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class Worker implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(Worker.class);

  private final Flow flow;
  private final FlusswerkMetrics metrics;
  private final MessageBroker messageBroker;
  private final ProcessReport processReport;
  private final PriorityBlockingQueue<Task> queue;
  private boolean running;
  private final Tracing tracing;

  public Worker(
      Flow flow,
      FlusswerkMetrics metrics,
      MessageBroker messageBroker,
      ProcessReport processReport,
      PriorityBlockingQueue<Task> queue,
      Tracing tracing) {
    this.flow = flow;
    this.messageBroker = messageBroker;
    this.metrics = metrics;
    this.processReport = processReport;
    this.queue = queue;
    this.tracing = tracing;
    this.running = true;
  }

  @Override
  public void run() {
    while (running) {
      step();
    }
  }

  /**
   * One step in the processing loop - wait for up to 1 second for a new task from the task queue
   * and process the message if there was one.
   */
  void step() {
    try {
      // Waiting with intervals so stopping the worker is possible
      Task task = queue.poll(1, TimeUnit.SECONDS);
      if (task == null) {
        return;
      }
      executeProcessing(task.getMessage());
      task.done();
    } catch (InterruptedException e) {
      LOGGER.debug("Interrupt while waiting for message", e);
    }
  }

  void executeProcessing(Message message) {
    metrics.incrementActiveWorkers();
    tracing.register(message.getTracing());
    process(message);
    tracing.deregister();
    metrics.decrementActiveWorkers();
  }

  public void process(Message message) {
    Collection<? extends Message> messagesToSend;
    SkipProcessingException skip = null;
    try {
      messagesToSend = flow.process(message);
      MDC.put("status", "success");
    } catch (StopProcessingException e) {
      MDC.put("status", "stop");
      fail(message, e);
      return; // processing was not successful → stop here
    } catch (SkipProcessingException e) {
      messagesToSend = e.getOutgoingMessages();
      skip = e;
      MDC.put("status", "skip");
      MDC.put("skipReason", e.getMessage());
    } catch (RuntimeException e) {
      MDC.put("status", "retry");
      if (e instanceof RetryProcessingException rpe && rpe.isComplex()) {
        complexRetry(message, rpe);
        return;
      } else {
        retryOrFail(message, e);
      }
      return; // processing was not successful → stop here
    }

    // Data processing was successful, now handle the messaging
    try {
      if (!messagesToSend.isEmpty()) {
        tracing.ensureFor(messagesToSend);
        messageBroker.send(messagesToSend);
      }
      messageBroker.ack(message);
      if (skip != null) {
        processReport.reportSkip(message, skip);
      } else {
        processReport.reportSuccess(message);
      }
    } catch (Exception e) {
      var stopProcessingException =
          new StopProcessingException("Could not finish message handling").causedBy(e);
      fail(message, stopProcessingException);
    }
  }

  private void retryOrFail(Message receivedMessage, RuntimeException e) {
    messageBroker.ack(receivedMessage);
    boolean isRejected = messageBroker.reject(receivedMessage);
    if (isRejected) {
      processReport.reportRetry(receivedMessage, e);
    } else {
      processReport.reportFailAfterMaxRetries(receivedMessage, e);
    }
  }

  private void complexRetry(Message receivedMessage, RetryProcessingException e) {
    messageBroker.ack(receivedMessage);
    boolean isRejected = false;
    for (Message retryMessage : e.getMessagesToRetry()) {
      Envelope envelope = retryMessage.getEnvelope();
      envelope.setRetries(receivedMessage.getEnvelope().getRetries());
      envelope.setSource(receivedMessage.getEnvelope().getSource());
      tracing.ensureFor(retryMessage);
      isRejected = messageBroker.reject(retryMessage);
    }
    // Send the messages that should be sent anyway
    tracing.ensureFor(e.getMessagesToSend());
    messageBroker.send(e.getMessagesToSend());

    if (isRejected) {
      processReport.reportComplexRetry(receivedMessage, e);
    } else {
      processReport.reportComplexFailedAfterMaxRetries(receivedMessage, e);
    }
  }

  private void fail(Message message, StopProcessingException e) {
    processReport.reportFail(message, e);
    messageBroker.fail(message);
  }

  /**
   * Stops processing new messages or waiting for new messages to arrive. This usually means that
   * the application will shut down when the last worker finished.
   */
  public void stop() {
    running = false;
    LOGGER.debug("Stopping engine...");
  }
}
