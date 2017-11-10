package org.mdz.dzp.workflow.neo.engine;

public class RabbitMQConfig {

  private String username = "guest";

  private String password = "guest";

  private String virtualHost = "/";

  private String hostName = "localhost";

  private int port = 5672;

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getVirtualHost() {
    return virtualHost;
  }

  public String getHostName() {
    return hostName;
  }

  public int getPort() {
    return port;
  }

}
