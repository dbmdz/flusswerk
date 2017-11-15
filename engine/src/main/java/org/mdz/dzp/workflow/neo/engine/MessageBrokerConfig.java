package org.mdz.dzp.workflow.neo.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mdz.dzp.workflow.neo.engine.jackson.DefaultMessageMixin;
import org.mdz.dzp.workflow.neo.engine.model.DefaultMessage;
import org.mdz.dzp.workflow.neo.engine.model.Message;

class MessageBrokerConfig {

  private String username;

  private String password;

  private String virtualHost;

  private String hostName;

  private int port;

  private ObjectMapper objectMapper;

  private int deadLetterWait;

  private int maxRetries;

  private Class<? extends Message> messageClass;

  private Class<?> messageMixin;

  private String exchange;

  private String deadLetterExchange;

  public MessageBrokerConfig() {
    maxRetries = 5;
    objectMapper = new ObjectMapper();
    messageClass = DefaultMessage.class;
    messageMixin = DefaultMessageMixin.class;
    exchange = "workflow";
    exchange = "dlx.workflow";
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

  public String getHost() {
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

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public int getDeadLetterWait() {
    return deadLetterWait;
  }

  public void setDeadLetterWait(int milliseconds) {
    this.deadLetterWait = milliseconds;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public Class<? extends Message> getMessageClass() {
    return messageClass;
  }

  public void setMessageClass(Class<? extends Message> messageClass) {
    this.messageClass = messageClass;
  }

  public Class<?> getMessageMixin() {
    return messageMixin;
  }

  public void setMessageMixin(Class<?> messageMixin) {
    this.messageMixin = messageMixin;
  }

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }

  public String getExchange() {
    return exchange;
  }

  public void setDeadLetterExchange(String deadLetterExchange) {
    this.deadLetterExchange = deadLetterExchange;
  }

  public String getDeadLetterExchange() {
    return deadLetterExchange;
  }

}
