package de.digitalcollections.workflow.engine.messagebroker;

class ConnectionConfigImpl implements ConnectionConfig {

  private String username;

  private String password;

  private String virtualHost;

  private String hostName;

  private int port;

  ConnectionConfigImpl() {
    setHostName("localhost");
    setPassword("guest");
    setPort(5672);
    setUsername("guest");
    setVirtualHost("/");
  }

  @Override
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String getVirtualHost() {
    return virtualHost;
  }

  public void setVirtualHost(String virtualHost) {
    this.virtualHost = virtualHost;
  }

  @Override
  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  @Override
  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

}
