package com.github.dbmdz.flusswerk.framework.rabbitmq;

import com.github.dbmdz.flusswerk.framework.config.properties.RabbitMQProperties;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(RabbitConnection.class);
  private static final int RETRY_INTERVAL = 5;

  private final ConnectionFactory factory;

  private Channel channel;
  private final String appName;

  private final RabbitMQProperties rabbitMQ;

  public RabbitConnection(RabbitMQProperties rabbitMQ, String appName) throws IOException {
    this(rabbitMQ, new ConnectionFactory(), appName);
  }

  RabbitConnection(RabbitMQProperties rabbitMQ, ConnectionFactory factory, String appName)
      throws IOException {
    this.rabbitMQ = rabbitMQ;
    this.factory = factory;
    this.appName = appName;
    factory.setUsername(rabbitMQ.getUsername());
    factory.setPassword(rabbitMQ.getPassword());
    rabbitMQ.getVirtualHost().ifPresent(factory::setVirtualHost);
    waitForConnection();
  }

  /**
   * Access to the low-level RabbitMQ {@link com.rabbitmq.client.Channel}. This is package protected
   * because users should always use the managed actions via {@link MessageBroker} or {@link
   * RabbitMQ}.
   *
   * @return the low-level RabbitMQ channel.
   */
  public Channel getChannel() {
    return channel;
  }

  final void waitForConnection() throws IOException {
    List<Address> addresses =
        rabbitMQ.getHosts().stream().map(Address::parseAddress).collect(Collectors.toList());
    boolean connectionIsFailing = true;
    while (connectionIsFailing) {
      try {
        LOGGER.debug("Waiting for connection to {} ...", addresses);
        Connection connection = factory.newConnection(addresses, appName);
        channel = connection.createChannel();
        channel.basicRecover(true);
        channel.basicQos(1);
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
