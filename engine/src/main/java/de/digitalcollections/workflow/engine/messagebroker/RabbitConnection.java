package de.digitalcollections.workflow.engine.messagebroker;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RabbitConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(RabbitConnection.class);
  private static final int RETRY_INTERVAL = 5;

  private final ConnectionFactory factory;

  private Channel channel;

  private final ConnectionConfig config;

  RabbitConnection(ConnectionConfig config) throws IOException {
    this(config, new ConnectionFactory());
  }

  RabbitConnection(ConnectionConfig config, ConnectionFactory factory) throws IOException {
    this.config = config;
    this.factory = factory;
    factory.setUsername(config.getUsername());
    factory.setPassword(config.getPassword());
    factory.setVirtualHost(config.getVirtualHost());

    waitForConnection();
  }

  public Channel getChannel() {
    return channel;
  }

  void waitForConnection() throws IOException {
    boolean connectionIsFailing = true;
    while (connectionIsFailing) {
      try {
        LOGGER.info("Waiting for connection...");
        Connection connection = factory.newConnection(config.getAddresses());
        channel = connection.createChannel();
        channel.basicRecover(true);
        connectionIsFailing = false;
        LOGGER.info("Connected");
      } catch (IOException | TimeoutException e) {
        LOGGER.debug("Could not connect", e);
        try {
          TimeUnit.SECONDS.sleep(RETRY_INTERVAL);
        } catch (InterruptedException e1) {
          throw new IOException("Could not connect to RabbitMQ", e);
        }
      }
    }
  }

  public boolean isOk() {
    channel.getConnection().isOpen();
    return true;
  }

}
