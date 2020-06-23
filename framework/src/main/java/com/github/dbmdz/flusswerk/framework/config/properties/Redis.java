package com.github.dbmdz.flusswerk.framework.config.properties;

import static java.util.Objects.requireNonNullElse;

public class Redis {

  private final String address;
  private final String password;

  public Redis(String address, String password) {
    this.address = requireNonNullElse(address, "redis://127.0.0.1:6379");
    this.password = password; // might be null if no authentication is used
  }

  public String getAddress() {
    return address;
  }

  public String getPassword() {
    return password;
  }
}
