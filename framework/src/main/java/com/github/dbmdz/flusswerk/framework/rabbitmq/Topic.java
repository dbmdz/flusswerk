package com.github.dbmdz.flusswerk.framework.rabbitmq;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

public class Topic {

  private final String name;
  private final MessageBroker messageBroker;

  Topic(String name, MessageBroker messageBroker) {
    this.name = requireNonNull(name);
    this.messageBroker = messageBroker;
  }

  public void send(Message message) throws IOException {
    messageBroker.send(name, message);
  }

  public void send(Collection<Message> messages) throws IOException {
    messageBroker.send(name, messages);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Topic) {
      Topic topic = (Topic) o;
      return Objects.equals(name, topic.name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return "Topic{name='" + name + "'}";
  }

  public String getName() {
    return name;
  }
}
