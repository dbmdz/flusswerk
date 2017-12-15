package de.digitalcollections.workflow.engine.messagebroker;

import com.fasterxml.jackson.databind.Module;
import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MessageBrokerConfigImpl implements MessageBrokerConfig {

  private int deadLetterWait;

  private int maxRetries;

  private List<Module> jacksonModules;

  private Class<? extends Message> messageClass;

  public MessageBrokerConfigImpl() {
    jacksonModules = new ArrayList<>();
    setMaxRetries(5);
    setDeadLetterWait(30 * 1000);
    setMessageClass(DefaultMessage.class);
  }

  @Override
  public int getDeadLetterWait() {
    return deadLetterWait;
  }

  public void setDeadLetterWait(int milliseconds) {
    this.deadLetterWait = milliseconds;
  }

  @Override
  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  @Override
  public Class<? extends Message> getMessageClass() {
    return messageClass;
  }

  public void setMessageClass(Class<? extends Message> messageClass) {
    this.messageClass = messageClass;
  }

  public void addJacksonModule(Module jacksonModule) {
    this.jacksonModules.add(jacksonModule);
  }

  @Override
  public List<Module> getJacksonModules() {
    return Collections.unmodifiableList(jacksonModules);
  }

}
