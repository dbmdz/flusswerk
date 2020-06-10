package com.github.dbmdz.flusswerk.framework.messagebroker;

import com.github.dbmdz.flusswerk.framework.config.properties.Connection;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(RabbitConnection.class);
  private static final int RETRY_INTERVAL = 5;

  private final ConnectionFactory factory;

  private Channel channel;

  private final Connection connection;

  public RabbitConnection(Connection connection) throws IOException {
    this(connection, new ConnectionFactory());
  }

  RabbitConnection(Connection connection, ConnectionFactory factory) throws IOException {
    this.connection = connection;
    this.factory = factory;
    factory.setUsername(connection.getUsername());
    factory.setPassword(connection.getPassword());
    factory.setVirtualHost(connection.getVirtualHost());

    waitForConnection();
  }

  public Channel getChannel() {
    return channel;
  }

  final void waitForConnection() throws IOException {
    boolean connectionIsFailing = true;
    while (connectionIsFailing) {
      List<Address> addresses = List.of(new Address(connection.getConnectTo()));
      try {
        LOGGER.debug("Waiting for connection to {} ...", addresses);
        com.rabbitmq.client.Connection connection = factory.newConnection(addresses);
        channel = connection.createChannel();
        channel.basicRecover(true);
        connectionIsFailing = false;
        LOGGER.debug("Connected to {}", addresses);
      } catch (IOException | TimeoutException e) {
        LOGGER.warn(
            "Could not connect to {}: {} {}",
            addresses,
            e.getClass().getSimpleName(),
            e.getMessage(),
            e);
        try {
          TimeUnit.SECONDS.sleep(RETRY_INTERVAL);
        } catch (InterruptedException e1) {
          throw new IOException("Could not connect to RabbitMQ at " + addresses, e);
        }
      }
    }
  }
}
