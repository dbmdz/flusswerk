package com.github.dbmdz.flusswerk.framework.messagebroker;

import com.fasterxml.jackson.databind.Module;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MessageBrokerConfig {

  private int deadLetterWait;

  private int maxRetries;

  private final List<Module> jacksonModules;

  private Class<? extends Message> messageClass;

  public MessageBrokerConfig() {
    jacksonModules = new ArrayList<>();
    setMaxRetries(5);
    setDeadLetterWait(30 * 1000);
    setMessageClass(Message.class);
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

  public Class<? extends Message> getMessageClass() {
    return messageClass;
  }

  public final void setMessageClass(Class<? extends Message> messageClass) {
    this.messageClass = messageClass;
  }

  public void addJacksonModule(Module jacksonModule) {
    this.jacksonModules.add(jacksonModule);
  }

  public List<Module> getJacksonModules() {
    return Collections.unmodifiableList(jacksonModules);
  }
}
