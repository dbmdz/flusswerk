package de.digitalcollections.workflow.engine.messagebroker;

public interface ConnectionConfig {

  String getUsername();

  String getPassword();

  String getVirtualHost();

  String getHostName();

  int getPort();
}
