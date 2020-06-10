package com.github.dbmdz.flusswerk.framework.config.properties;

import javax.validation.constraints.NotBlank;

/** Connection information for RabbitMQ. */
public class Connection {

  @NotBlank private final String connectTo;

  private final String virtualHost;

  @NotBlank private final String username;

  @NotBlank private final String password;

  /**
   * @param connectTo The RabbitMQ connection String
   * @param virtualHost The RabbitMQ/AMQP virtual host. <em>Can be null.</em>
   * @param username The username for RabbitMQ login
   * @param password The password for RabbitMQ login
   */
  public Connection(
      @NotBlank String connectTo,
      String virtualHost,
      @NotBlank String username,
      @NotBlank String password) {
    this.connectTo = connectTo;
    this.virtualHost = virtualHost;
    this.username = username;
    this.password = password;
  }

  /** @return The RabbitMQ connection String */
  public String getConnectTo() {
    return connectTo;
  }

  /** @return The RabbitMQ/AMQP virtual host. <em>Can be null.</em> */
  public String getVirtualHost() {
    return virtualHost;
  }

  /** @return The username for RabbitMQ login */
  public String getUsername() {
    return username;
  }

  /** @return The password for RabbitMQ login */
  public String getPassword() {
    return password;
  }

  @Override
  public String toString() {
    return StringRepresentation.of(Connection.class)
        .property("connectTo", connectTo)
        .property("virtualHost", virtualHost)
        .property("username", username)
        .maskedProperty("password", password)
        .toString();
  }
}
