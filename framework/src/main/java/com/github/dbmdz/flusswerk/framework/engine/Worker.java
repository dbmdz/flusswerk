package com.github.dbmdz.flusswerk.framework.engine;

import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.MessageBroker;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Worker implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(Worker.class);

  private final Flow flow;
  private final MessageBroker messageBroker;
  private final ProcessReport processReport;
  private final PriorityBlockingQueue<Task> queue;
  private boolean running;
  private final Tracing tracing;

  public Worker(
      Flow flow,
      MessageBroker messageBroker,
      ProcessReport processReport,
      PriorityBlockingQueue<Task> queue,
      Tracing tracing) {
    this.flow = flow;
    this.messageBroker = messageBroker;
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
    tracing.register(message.getTracing());
    process(message);
    tracing.deregister();
  }

  public void process(Message message) {
    Collection<? extends Message> messagesToSend;
    try {
      messagesToSend = flow.process(message);
    } catch (StopProcessingException e) {
      fail(message, e);
      return; // processing was not successful → stop here
    } catch (RuntimeException e) {
      retryOrFail(message, e);
      return; // processing was not successful → stop here
    }

    // Data processing was successful, now handle the messaging
    try {
      if (!messagesToSend.isEmpty()) {
        messageBroker.send(messagesToSend);
      }
      messageBroker.ack(message);
      processReport.reportSuccess(message);
    } catch (Exception e) {
      var stopProcessingException =
          new StopProcessingException("Could not finish message handling").causedBy(e);
      fail(message, stopProcessingException);
    }
  }

  private void retryOrFail(Message receivedMessage, RuntimeException e) {
    try {
      boolean isRejected = messageBroker.reject(receivedMessage);
      if (isRejected) {
        processReport.reportReject(receivedMessage, e);
      } else {
        processReport.reportFailAfterMaxRetries(receivedMessage, e);
      }
    } catch (IOException fatalException) {
      var body = receivedMessage.getEnvelope().getBody();
      LOGGER.error("Could not reject message" + body, fatalException);
    }
  }

  private void fail(Message message, StopProcessingException e) {
    try {
      processReport.reportFail(message, e);
      messageBroker.fail(message);
    } catch (IOException fatalException) {
      var body = message.getEnvelope().getBody();
      LOGGER.error("Could not fail message" + body, fatalException);
    }
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
