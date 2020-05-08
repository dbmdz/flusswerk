package com.github.dbmdz.flusswerk.framework.messagebroker.builder;

import static java.util.Objects.requireNonNull;

import com.rabbitmq.client.Address;
import java.util.List;

public class RabbitMQ {

  private final List<Address> addresses;
  private String username;
  private String password;
  private String virtualHost;
  private String exchange;

  RabbitMQ(List<Address> addresses) {
    this.addresses = addresses;
  }

  public static RabbitMQ host(String hostname, int port) {
    return new RabbitMQ(List.of(new Address(hostname, port)));
  }

  public static RabbitMQ hosts(List<Address> addresses) {
    return new RabbitMQ(addresses);
  }

  public RabbitMQ auth(String username, String password) {
    this.username = username;
    this.password = password;
    return this;
  }

  public RabbitMQ virtualHost(String virtualHost) {
    this.virtualHost = requireNonNull(virtualHost);
    return this;
  }

  public RabbitMQ exchange(String exchange) {
    this.exchange = exchange;
    return this;
  }

  public List<Address> getAddresses() {
    return addresses;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getVirtualHost() {
    return virtualHost;
  }

  public String getExchange() {
    return exchange;
  }
}
