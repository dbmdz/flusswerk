package com.github.dbmdz.flusswerk.framework.rabbitmq;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import java.util.Collection;

public class Topic {

  private final String name;
  private final MessageBroker messageBroker;

  Topic(String name, MessageBroker messageBroker) {
    this.name = name;
    this.messageBroker = messageBroker;
  }

  public void send(Message message) throws IOException {
    messageBroker.send(name, message);
  }

  public void send(Collection<Message> messages) throws IOException {
    messageBroker.send(name, messages);
  }

}
