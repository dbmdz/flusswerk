package com.github.dbmdz.flusswerk.framework.rabbitmq;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;

public class Processing {

  private MessageBroker messageBroker;

  public Processing(MessageBroker messageBroker) {
    this.messageBroker = messageBroker;
  }

  public void stop(Message message) throws IOException {
    messageBroker.fail(message);
  }

  public boolean retry(Message message) throws IOException {
    return messageBroker.reject(message);
  }
}
