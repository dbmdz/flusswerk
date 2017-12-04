package de.digitalcollections.workflow.engine;

public class ConnectionConfig {

  private String username;

  private String password;

  private String virtualHost;

  private String hostName;

  private int port;

  public ConnectionConfig() {
    setHostName("localhost");
    setPassword("guest");
    setPort(5672);
    setUsername("guest");
    setVirtualHost("/");
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getVirtualHost() {
    return virtualHost;
  }

  public void setVirtualHost(String virtualHost) {
    this.virtualHost = virtualHost;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

}
