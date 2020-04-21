package de.digitalcollections.flusswerk.engine.messagebroker;

import com.fasterxml.jackson.databind.Module;
import de.digitalcollections.flusswerk.engine.model.Message;
import java.util.List;

public interface MessageBrokerConfig {

  int getDeadLetterWait();

  int getMaxRetries();

  Class<? extends Message> getMessageClass();

  List<Module> getJacksonModules();
}
