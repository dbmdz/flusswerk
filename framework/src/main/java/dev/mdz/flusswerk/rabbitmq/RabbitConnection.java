package dev.mdz.flusswerk.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.recovery.*;
import com.rabbitmq.utility.Utility;
import dev.mdz.flusswerk.LifecyclePhases;
import dev.mdz.flusswerk.config.properties.RabbitMQProperties;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

public class RabbitConnection implements SmartLifecycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(RabbitConnection.class);
  private static final int RETRY_INTERVAL = 5;

  private final ConnectionFactory factory;

  private AutorecoveringConnection connection;

  private AutorecoveringChannel channel;
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
    factory.setUsername(rabbitMQ.username());
    factory.setPassword(rabbitMQ.password());
    rabbitMQ.getVirtualHost().ifPresent(factory::setVirtualHost);
    factory.setConnectionRecoveryTriggeringCondition(sse -> !sse.isInitiatedByApplication());
    waitForConnection(); // not in start() because this should fail fast if no connection is
    // possible
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

  private void waitForConnection() throws IOException {
    List<Address> addresses =
        rabbitMQ.hosts().stream().map(Address::parseAddress).collect(Collectors.toList());
    boolean connectionIsFailing = true;
    while (connectionIsFailing) {
      try {
        LOGGER.debug("Waiting for connection to {} ...", addresses);
        connection = (AutorecoveringConnection) factory.newConnection(addresses, appName);
        channel = (AutorecoveringChannel) connection.createChannel();
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

  public void recoverChannel() throws IOException {
    channel.automaticallyRecover(connection, connection.getDelegate());
    // recover topology
    for (final RecordedExchange exchange :
        Utility.copy(connection.getRecordedExchanges()).values()) {
      if (exchange.getChannel() == channel) {
        connection.recoverExchange(exchange, true);
      }
    }
    for (final Map.Entry<String, RecordedQueue> entry :
        Utility.copy(connection.getRecordedQueues()).entrySet()) {
      if (entry.getValue().getChannel() == channel) {
        connection.recoverQueue(entry.getKey(), entry.getValue(), true);
      }
    }
    for (final RecordedBinding b : Utility.copy(connection.getRecordedBindings())) {
      if (b.getChannel() == channel) {
        connection.recoverBinding(b, true);
      }
    }
  }

  public void close() {
    if (connection == null || !connection.isOpen()) {
      return;
    }
    try {
      connection.close(AMQP.CONNECTION_FORCED, "Application shutdown");
    } catch (IOException exception) {
      LOGGER.error("Could not close RabbitMQ connection", exception);
    }
  }

  @Override
  public int getPhase() {
    return LifecyclePhases.CONNECTIONS; // should be started before any consumers
  }

  @Override
  public boolean isAutoStartup() {
    return true; // use lifecycle management
  }

  @Override
  public boolean isRunning() {
    return connection != null && connection.isOpen();
  }

  @Override
  public void start() {
    // nothing to do, connection is established in constructor for fail-fast behavior
    LOGGER.info("Starting RabbitMQ connection");
  }

  @Override
  public void stop() {
    LOGGER.info("Closing RabbitMQ connection");
    close();
  }
}
