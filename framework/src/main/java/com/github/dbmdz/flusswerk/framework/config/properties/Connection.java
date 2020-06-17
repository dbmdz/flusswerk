package com.github.dbmdz.flusswerk.framework.config.properties;

import static java.util.Objects.requireNonNullElse;

import java.util.Optional;
import javax.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConstructorBinding;

/** Connection information for RabbitMQ. */
@ConstructorBinding
public class Connection {

  @NotBlank private final String host;

  private final int port;

  private final String virtualHost;

  @NotBlank private final String username;

  @NotBlank private final String password;

  /**
   * @param host The RabbitMQ host name
   * @param port The RabbitMQ port
   * @param virtualHost The RabbitMQ/AMQP virtual host. <em>Can be null.</em>
   * @param username The username for RabbitMQ login
   * @param password The password for RabbitMQ login
   */
  public Connection(
      @NotBlank String host,
      Integer port,
      String virtualHost,
      @NotBlank String username,
      @NotBlank String password) {
    this.host = host;
    this.port = requireNonNullElse(port, 5672);
    this.virtualHost = virtualHost;
    this.username = username;
    this.password = password;
  }

  /** @return RabbitMQ host name to connect to. */
  public String getHost() {
    return host;
  }

  /** @return RabbitMQ port to connect to (default: 5672). */
  public int getPort() {
    return port;
  }

  /** @return The RabbitMQ/AMQP virtual host. <em>Can be null.</em> */
  public Optional<String> getVirtualHost() {
    return Optional.ofNullable(virtualHost);
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
        .property("host", host)
        .property("port", port)
        .property("virtualHost", virtualHost)
        .property("username", username)
        .maskedProperty("password", password)
        .toString();
  }
}
