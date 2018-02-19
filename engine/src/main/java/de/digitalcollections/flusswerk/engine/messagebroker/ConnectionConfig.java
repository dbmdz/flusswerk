package de.digitalcollections.flusswerk.engine.messagebroker;

import com.rabbitmq.client.Address;
import java.util.List;

public interface ConnectionConfig {

  String getUsername();

  String getPassword();

  String getVirtualHost();

  List<Address> getAddresses();

}
