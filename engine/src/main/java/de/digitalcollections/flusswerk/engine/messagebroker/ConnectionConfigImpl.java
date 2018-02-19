package de.digitalcollections.flusswerk.engine.messagebroker;

import com.rabbitmq.client.Address;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ConnectionConfigImpl implements ConnectionConfig {

  private String username;

  private String password;

  private String virtualHost;

  private List<Address> addresses;

  ConnectionConfigImpl() {
    addresses = new ArrayList<>();
    setPassword("guest");
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

  public void addAddress(String host, int port) {
    addresses.add(new Address(host, port));
  }

  @Override
  public List<Address> getAddresses() {
    if (addresses.isEmpty()) {
      return Collections.singletonList(new Address("localhost", 5672));
    }
    return addresses;
  }
}
