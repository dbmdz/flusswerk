package com.github.dbmdz.flusswerk.framework.messagebroker;

import com.rabbitmq.client.Address;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConnectionConfig {

  private String username;

  private String password;

  private String virtualHost;

  private final List<Address> addresses;

  public ConnectionConfig() {
    addresses = new ArrayList<>();
    setPassword("guest");
    setUsername("guest");
    setVirtualHost("/");
  }

  public String getUsername() {
    return username;
  }

  public final void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public final void setPassword(String password) {
    this.password = password;
  }

  public String getVirtualHost() {
    return virtualHost;
  }

  public final void setVirtualHost(String virtualHost) {
    this.virtualHost = virtualHost;
  }

  public void addAddress(String host, int port) {
    addresses.add(new Address(host, port));
  }

  public List<Address> getAddresses() {
    if (addresses.isEmpty()) {
      return Collections.singletonList(new Address("localhost", 5672));
    }
    return addresses;
  }

  public void setAddresses(List<Address> addresses) {
    this.addresses.clear();
    this.addresses.addAll(addresses);
  }
}
