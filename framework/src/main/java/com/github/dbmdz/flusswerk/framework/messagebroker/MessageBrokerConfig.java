package com.github.dbmdz.flusswerk.framework.messagebroker;

import com.fasterxml.jackson.databind.Module;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.List;

public interface MessageBrokerConfig {

  int getDeadLetterWait();

  int getMaxRetries();

  Class<? extends Message> getMessageClass();

  List<Module> getJacksonModules();
}
