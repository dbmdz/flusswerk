package de.digitalcollections.workflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.digitalcollections.workflow.engine.jackson.DefaultMessageMixin;
import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;

class MessageBrokerConfig {

  private ObjectMapper objectMapper;

  private int deadLetterWait;

  private int maxRetries;

  private Class<? extends Message> messageClass;

  private Class<?> messageMixin;

  private RoutingConfig routingConfig;

  public MessageBrokerConfig() {
    this.routingConfig = new RoutingConfig();
    setObjectMapper(new ObjectMapper());
    setMaxRetries(5);
    setDeadLetterWait(30 * 1000);
    setObjectMapper(new ObjectMapper());
    setMessageClass(DefaultMessage.class);
    setMessageMixin(DefaultMessageMixin.class);
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
