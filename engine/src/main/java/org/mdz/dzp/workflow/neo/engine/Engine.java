package org.mdz.dzp.workflow.neo.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.mdz.dzp.workflow.neo.engine.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Engine {

  private static final Logger LOGGER = LoggerFactory.getLogger(Engine.class);

  private static final boolean SINGLE_MESSAGE = false;

  private static final boolean DONT_REQUEUE = false;

  private static final boolean NO_AUTOACK = false;

  private final RabbitMQ rabbitMQ;

  private final Flow<?, ?> flow;

  private final ExecutorService executorService;

  private final Semaphore semaphore;

  private final ObjectMapper objectMapper;

  public Engine(RabbitMQ rabbitMQ, Flow<?, ?> flow) throws IOException {
    this.rabbitMQ = rabbitMQ;
    this.flow = flow;
    this.executorService = Executors.newFixedThreadPool(5);
    this.semaphore = new Semaphore(5);
    this.objectMapper = new ObjectMapper();

    rabbitMQ.provideInputQueue(flow.getInputChannel());
    rabbitMQ.provideOutputQueue(flow.getOutputChannel());
  }

  private String string(byte[] data) {
    try {
      return new String(data, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOGGER.error("Could not convert byte[] to String because charset UTF-8 is not supported");
      return null;
    }
  }

  public void start() {
    final Channel channel = rabbitMQ.getChannel();
    while (true) {
      try {
        semaphore.acquire();

        GetResponse response = channel.basicGet(flow.getInputChannel(), NO_AUTOACK);

        if (response == null) {
          LOGGER.info("Checking for new message (available semaphores: {}) - Queue is empty", semaphore.availablePermits());
          TimeUnit.SECONDS.sleep(1);
          semaphore.release();
          continue;
        }

        LOGGER.info("Checking for new message (available semaphores: {}), got {}", semaphore.availablePermits(), string(response.getBody()));

        executorService.execute(() -> {
          try {
            Message message = objectMapper.readValue(response.getBody(), Message.class);
            Message result = flow.process(message);
            if (flow.hasOutputChannel()) {
              send(result);
            }
            channel.basicAck(response.getEnvelope().getDeliveryTag(), SINGLE_MESSAGE);
          } catch (RuntimeException | IOException  e) {
            try {
              LOGGER.error("Could not process message: {}", string(response.getBody()));
              channel.basicReject(response.getEnvelope().getDeliveryTag(), DONT_REQUEUE);
            } catch (IOException e1) {
              LOGGER.error("Could not reject message" + string(response.getBody()), e1);
            }
          }
          semaphore.release();
        });

      } catch (IOException | InterruptedException e) {
        LOGGER.error("Got some error", e);
      }
    }
  }

  private void send(Message result) throws IOException {
    AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
        .contentType("application/json")
        .deliveryMode(2)
        .build();
    rabbitMQ.getChannel().basicPublish("testExchange", flow.getOutputChannel(), properties, objectMapper.writeValueAsBytes(result));
  }

  public void createTestMessages() throws IOException {
    final int n = 500;
    for (int i = 0; i < n; i++) {
      String message = String.format("Test message #%d of %d", i, n);
      rabbitMQ.send(new Message(message));
    }
  }

}
