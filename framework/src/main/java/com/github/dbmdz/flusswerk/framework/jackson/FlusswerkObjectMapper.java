package com.github.dbmdz.flusswerk.framework.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.dbmdz.flusswerk.framework.model.Envelope;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.model.Message;

public class FlusswerkObjectMapper extends ObjectMapper {

  private final Class<? extends Message> messageClass;

  public FlusswerkObjectMapper(IncomingMessageType incomingMessageType) {
    messageClass = incomingMessageType.getMessageClass();
    if (incomingMessageType.hasMixin()) {
      addMixIn(incomingMessageType.getMessageClass(), incomingMessageType.getMixin());
    } else {
      addMixIn(Message.class, DefaultMixin.class);
    }
    addMixIn(Envelope.class, EnvelopeMixin.class);
    registerModule(new JavaTimeModule());
  }

  public Message deserialize(String json) throws JsonProcessingException {
    return readValue(json, messageClass);
  }
}
