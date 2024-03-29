package com.github.dbmdz.flusswerk.framework.config.properties;

import static java.util.Objects.requireNonNullElse;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Connection information for RabbitMQ. */
@ConfigurationProperties(prefix = "flusswerk.rabbitmq")
public record RabbitMQProperties(
    List<String> hosts, String virtualHost, String username, String password) {

  /**
   * @param hosts The RabbitMQ host names. May include a specific port separated by ":" (default:
   *     5672).
   * @param virtualHost The RabbitMQ/AMQP virtual host. <em>Can be null.</em>
   * @param username The username for RabbitMQ login
   * @param password The password for RabbitMQ login
   */
  public RabbitMQProperties {
    hosts = requireNotEmpty(hosts, List.of("localhost"));
    username = requireNonNullElse(username, "guest");
    password = requireNonNullElse(password, "guest");
  }

  /**
   * @return The RabbitMQ/AMQP virtual host. <em>Can be null.</em>
   */
  public Optional<String> getVirtualHost() {
    return Optional.ofNullable(virtualHost);
  }

  @Override
  public String toString() {
    return String.format(
        "RabbitMQProperties{hosts=%s, username=%s, pasword=*****}", hosts, username);
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
