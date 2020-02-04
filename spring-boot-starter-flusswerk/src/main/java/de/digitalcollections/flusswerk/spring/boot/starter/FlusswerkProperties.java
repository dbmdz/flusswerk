package de.digitalcollections.flusswerk.spring.boot.starter;

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

  /** Configuration related to the processing. */
  static class Processing {

    @Min(0)
    private int maxRetries;

    @Min(1)
    private int threads;

    public Processing(@Min(0) int maxRetries, @Min(1) int threads) {
      this.maxRetries = maxRetries;
      this.threads = threads;
    }

    /** @return the maximum number of retries before a message ends up in the failed queue. */
    public int getMaxRetries() {
      return maxRetries;
    }

    /** @return The number of concurrent processing threads in one job instance. */
    public int getThreads() {
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
  static class Connection {

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
  static class Routing {

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

  @ConstructorBinding
  public FlusswerkProperties(Processing processing, Connection connection, Routing routing) {
    this.processing = processing;
    this.connection = connection;
    this.routing = routing;
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

  @Override
  public String toString() {
    return StringRepresentation.of(FlusswerkProperties.class)
        .property("processing", processing.toString())
        .property("routing", routing.toString())
        .property("connection", connection.toString())
        .toString();
  }
}
