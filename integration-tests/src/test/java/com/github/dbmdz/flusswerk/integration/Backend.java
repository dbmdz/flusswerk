package com.github.dbmdz.flusswerk.integration;

import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.messagebroker.builder.BuildStep;
import com.github.dbmdz.flusswerk.framework.messagebroker.builder.MessageBrokerBuilder;
import com.github.dbmdz.flusswerk.framework.messagebroker.builder.RabbitMQ;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Backend {

  private static final long MAX_WAIT = 60 * 1000;

  private static final long INTERVAL = 100;

  private MessageBroker<Message> messageBroker;

  public Backend(String readFrom, String writeTo) {
    String host = getEnvOrDefault("RABBIT_HOST", "localhost");
    int port = Integer.parseInt(getEnvOrDefault("RABBIT_PORT", "5672"));

    long totalWait = 0;
    BuildStep<Message> buildStep =
        MessageBrokerBuilder.read(Message.class)
            .from(readFrom)
            .waitBetweenRetries(1) // no need to wait for tests
            .sendTo(writeTo)
            .via(RabbitMQ.host(host, port));

    messageBroker = null;
    while (messageBroker == null) {
      try {
        messageBroker = buildStep.build();
      } catch (RuntimeException e) {
        try {
          TimeUnit.MILLISECONDS.sleep(INTERVAL);
          totalWait += INTERVAL;
        } catch (InterruptedException e1) {
          throw new IllegalStateException(e1);
        }
      }
      if (totalWait > MAX_WAIT) {
        throw new IllegalStateException(
            "Waited for "
                + totalWait / 1000
                + " seconds but got no suitable connection to Backend");
      }
    }
  }

  private String getEnvOrDefault(String name, String defaultValue) {
    String value = System.getenv(name);
    if (value == null) {
      return defaultValue;
    } else {
      return value;
    }
  }

  /**
   * Wait to receive a message, return null if there is none in 5 seconds.
   *
   * @param queue The queue name to read from
   * @return a message or null there is none
   * @throws IOException if reading from RabbitMQ fails
   * @throws InterruptedException if waiting is interrupted
   * @throws InvalidMessageException if the message cannot be parsed
   */
  public Message waitForMessageFrom(String queue, int timeout)
      throws IOException, InterruptedException, InvalidMessageException {
    Message message = null;
    long time = System.currentTimeMillis();
    while (message == null && (System.currentTimeMillis() - time < timeout)) {
      message = messageBroker.receive(queue);
      if (message == null) {
        TimeUnit.MILLISECONDS.sleep(50);
      } else {
        messageBroker.ack(message);
      }
    }

    return message;
  }

  public MessageBroker getMessageBroker() {
    return messageBroker;
  }

  //  /**
  //   * Executes a test plan an returns a message. Creates fresh {@link MessageBroker} and {@link
  // Engine} instances for each call.
  //   * @param plan The plan
  //   * @return a message or null if there is none
  //   * @throws IOException if reading from RabbitMQ fails
  //   * @throws InterruptedException if waiting is interrupted
  //   * @throws InvalidMessageException if the message cannot be parsed
  //   */
  //  public Map<String, DefaultMessage> execute(Plan plan) throws IOException,
  // InterruptedException, InvalidMessageException, URISyntaxException {
  //    MessageBroker messageBroker = createMessageBroker(plan.getQueueIn(), plan.getQueueOut());
  //    Engine engine = new Engine(messageBroker, plan.getFlow());
  //
  //    ExecutorService executorService = Executors.newSingleThreadExecutor();
  //    executorService.submit(engine::start);
  //    messageBroker.send(plan.getQueueIn(), plan.getMessages());
  //    Thread.sleep(5000);
  //
  //    String url = String.format("http://%s:%d", HOST, MANAGEMENT_PORT);
  //    Client client = new Client(url, "guest", "guest");
  //
  //    Map<String, DefaultMessage> messages = new HashMap<>();
  //    for (QueueInfo queue : client.getQueues()) {
  //      DefaultMessage message = (DefaultMessage) waitForMessageFrom(messageBroker,
  // queue.getName());
  //      messages.put(queue.getName(), message);
  //      client.deleteQueue(queue.getVhost(), queue.getName());
  //    }
  //
  //    plan.setResult(messages);
  //
  //    engine.stop();
  //    executorService.shutdownNow();
  //
  //    return messages;
  //  }

}
