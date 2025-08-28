package dev.mdz.flusswerk.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import dev.mdz.flusswerk.model.Envelope;
import dev.mdz.flusswerk.model.IncomingMessageType;
import dev.mdz.flusswerk.model.Message;

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
    registerModule(new ParameterNamesModule());
    registerModule(new Jdk8Module());
  }

  public Message deserialize(String json) throws JsonProcessingException {
    return readValue(json, messageClass);
  }

  /**
   * Convenience factory method to prevent overly long lines.
   *
   * @param type The class for incoming messages
   * @return a new instance of FlusswerkObjectMapper.
   */
  public static FlusswerkObjectMapper forIncoming(Class<? extends Message> type) {
    return new FlusswerkObjectMapper(new IncomingMessageType(type));
  }
}
