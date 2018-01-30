package de.digitalcollections.workflow.engine.messagebroker;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

class RabbitConnection {

  private final Connection connection;

  private final Channel channel;

  RabbitConnection(ConnectionConfig config) throws IOException, TimeoutException {
    this(config, new ConnectionFactory());
  }

  RabbitConnection(ConnectionConfig config, ConnectionFactory factory) throws IOException, TimeoutException {
    factory.setUsername(config.getUsername());
    factory.setPassword(config.getPassword());
    factory.setVirtualHost(config.getVirtualHost());
    this.connection = factory.newConnection(config.getAddresses());
    this.channel = connection.createChannel();
  }

  public Connection getConnection() {
    return connection;
  }

  public Channel getChannel() {
    return channel;
  }

}
