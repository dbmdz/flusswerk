package com.github.dbmdz.flusswerk.spring.boot.starter;

import java.util.Objects;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/** Model for the configuration parameters in <code>application.yml</code>. */
@ConstructorBinding
@ConfigurationProperties(prefix = "flusswerk")
public class FlusswerkProperties {

  private Processing processing;

  private Connection connection;

  private Routing routing;

  private Monitoring monitoring;

  /** Configuration related to the processing. */
  public static class Processing {

    @Min(0)
    private Integer maxRetries;

    @Min(1)
    private Integer threads;

    public Processing(@Min(0) Integer maxRetries, @Min(1) Integer threads) {
      this.maxRetries = maxRetries;
      this.threads = threads;
    }

    /** @return the maximum number of retries before a message ends up in the failed queue. */
    public Integer getMaxRetries() {
      return maxRetries;
    }

    /** @return The number of concurrent processing threads in one job instance. */
    public Integer getThreads() {
      return threads;
    }

    @Override
    public String toString() {
      return StringRepresentation.of(Processing.class)
          .property("maxRetries", maxRetries)
          .property("threads", threads)
          .toString();
    }
  }

  /** Connection information for RabbitMQ. */
  public static class Connection {

    @NotBlank private String connectTo;

    private String virtualHost;

    @NotBlank private String username;

    @NotBlank private String password;

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

  /** AMQP/RabbitMQ routing information. */
  public static class Routing {

    @NotBlank private String exchange;

    private String[] readFrom;

    private String writeTo;

    /**
     * @param exchange The exchange name to use (required).
     * @param readFrom The queue to read from (optional).
     * @param writeTo The topic to send to per default (optional).
     */
    public Routing(@NotBlank String exchange, String[] readFrom, String writeTo) {
      this.exchange = exchange;
      this.readFrom = readFrom;
      this.writeTo = writeTo;
    }

    /** @return The exchange name to use (required). */
    public String getExchange() {
      return exchange;
    }

    /** @return The queue to read from (optional). */
    public String[] getReadFrom() {
      return readFrom;
    }

    /** @return The topic to send to per default (optional). */
    public String getWriteTo() {
      return writeTo;
    }

    @Override
    public String toString() {
      return StringRepresentation.of(Routing.class)
          .property("exchange", exchange)
          .property("readFrom", String.join(",", readFrom))
          .property("writeTo", writeTo)
          .toString();
    }
  }

  /** Settings for monitoring endpoints. */
  public static class Monitoring {

    private String prefix;

    public Monitoring(String prefix) {
      this.prefix = Objects.requireNonNullElse(prefix, "");
    }

    public String getPrefix() {
      return prefix;
    }

    @Override
    public String toString() {
      return StringRepresentation.of(Monitoring.class).property("prefix", prefix).toString();
    }
  }

  @ConstructorBinding
  public FlusswerkProperties(
      Processing processing, Connection connection, Routing routing, Monitoring monitoring) {
    this.processing = processing;
    this.connection = connection;
    this.routing = routing;
    this.monitoring = monitoring;
  }

  public Processing getProcessing() {
    return processing;
  }

  public Connection getConnection() {
    return connection;
  }

  public Routing getRouting() {
    return routing;
  }

  public Monitoring getMonitoring() {
    return monitoring;
  }

  @Override
  public String toString() {
    return StringRepresentation.of(FlusswerkProperties.class)
        .property("processing", processing.toString())
        .property("routing", routing.toString())
        .property("connection", connection.toString())
        .property("monitoring", monitoring.toString())
        .toString();
  }
}
