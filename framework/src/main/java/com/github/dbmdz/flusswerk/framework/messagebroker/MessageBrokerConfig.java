package com.github.dbmdz.flusswerk.framework.messagebroker;

import com.fasterxml.jackson.databind.Module;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageBrokerConfig<M extends Message> {

  private int deadLetterWait;

  private int maxRetries;

  private final List<Module> jacksonModules;

  private Class<M> messageClass;

  public MessageBrokerConfig(Class<M> messageClass) {
    jacksonModules = new ArrayList<>();
    setMaxRetries(5);
    setDeadLetterWait(30 * 1000);
    this.messageClass = messageClass;
  }

  public MessageBrokerConfig(int deadLetterWait, int maxRetries,
      List<Module> jacksonModules, Class<M> messageClass) {
    this.deadLetterWait = deadLetterWait;
    this.maxRetries = maxRetries;
    this.jacksonModules = jacksonModules;
    this.messageClass = messageClass;
  }

  public int getDeadLetterWait() {
    return deadLetterWait;
  }

  public final void setDeadLetterWait(int milliseconds) {
    this.deadLetterWait = milliseconds;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public final void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public Class<M> getMessageClass() {
    return messageClass;
  }

  public final void setMessageClass(Class<M> messageClass) {
    this.messageClass = messageClass;
  }

  public void addJacksonModule(Module jacksonModule) {
    this.jacksonModules.add(jacksonModule);
  }

  public List<Module> getJacksonModules() {
    return Collections.unmodifiableList(jacksonModules);
  }
}
