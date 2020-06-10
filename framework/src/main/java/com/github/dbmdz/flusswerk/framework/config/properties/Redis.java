package com.github.dbmdz.flusswerk.framework.config.properties;

import static java.util.Objects.requireNonNullElse;

public class Redis {

  private final String host;
  private final int port;
  private final String secret;

  public Redis(String host, Integer port, String secret) {
    this.host = host;
    this.port = requireNonNullElse(port, 6379);
    this.secret = secret;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getSecret() {
    return secret;
  }
}
