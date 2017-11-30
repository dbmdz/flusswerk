package de.digitalcollections.workflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.digitalcollections.workflow.engine.jackson.DefaultMessageMixin;
import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;

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

  private RoutingConfig routingConfig;

  public MessageBrokerConfig() {
    this.routingConfig = new RoutingConfig();
    setHostName("localhost");
    setPassword("guest");
    setPort(5672);
    setUsername("guest");
    setVirtualHost("/");
    setObjectMapper(new ObjectMapper());
    setMaxRetries(5);
    setDeadLetterWait(30 * 1000);
    setExchange("workflow");
    setDeadLetterExchange("workflow.retry");
    setObjectMapper(new ObjectMapper());
    setMessageClass(DefaultMessage.class);
    setMessageMixin(DefaultMessageMixin.class);
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
    routingConfig.setExchange(exchange);
  }

  public void setDeadLetterExchange(String deadLetterExchange) {
    routingConfig.setDeadLetterExchange(deadLetterExchange);
  }

  public void setReadFrom(String readFrom) {
    routingConfig.setReadFrom(readFrom);
  }

  public void setWriteTo(String writeTo) {
    routingConfig.setWriteTo(writeTo);
  }

  public RoutingConfig getRoutingConfig() {
    return routingConfig;
  }
}
