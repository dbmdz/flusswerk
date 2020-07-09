package com.github.dbmdz.flusswerk.framework.config.properties;

import static java.util.Objects.requireNonNullElse;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.context.properties.ConstructorBinding;

/** Connection information for RabbitMQ. */
@ConstructorBinding
public class RabbitMQProperties {

  private final List<String> hosts;
  private final String virtualHost;
  private final String username;
  private final String password;

  /**
   * @param hosts The RabbitMQ host names. May include a specific port separated by ":" (default:
   *     5672).
   * @param virtualHost The RabbitMQ/AMQP virtual host. <em>Can be null.</em>
   * @param username The username for RabbitMQ login
   * @param password The password for RabbitMQ login
   */
  public RabbitMQProperties(
      List<String> hosts, String virtualHost, String username, String password) {
    this.hosts = requireNotEmpty(hosts, List.of("localhost"));
    this.virtualHost = virtualHost; // can actually be null
    this.username = requireNonNullElse(username, "guest");
    this.password = requireNonNullElse(password, "guest");
  }

  /** @return The connection hosts to RabbitMQ. May include a specific port separated by ":". */
  public List<String> getHosts() {
    return hosts;
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
    return StringRepresentation.of(RabbitMQProperties.class)
        .property("hosts", hosts)
        .property("virtualHost", virtualHost)
        .property("username", username)
        .maskedProperty("password", password)
        .toString();
  }

  public static RabbitMQProperties defaults() {
    // use null values so constructor sets defaults
    return new RabbitMQProperties(null, null, null, null);
  }

  private static <T> List<T> requireNotEmpty(List<T> list, List<T> defaultValues) {
    if (list == null || list.isEmpty()) {
      return defaultValues;
    }
    return list;
  }
}
