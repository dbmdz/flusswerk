package com.github.dbmdz.flusswerk.framework.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.dbmdz.flusswerk.framework.model.DefaultMessage;
import com.github.dbmdz.flusswerk.framework.model.Envelope;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultMessageMixinTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.addMixIn(Message.class, DefaultMessageMixin.class);
    objectMapper.addMixIn(Envelope.class, EnvelopeMixin.class);
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.enable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
  }

  @Test
  @DisplayName("Serialization should exclude field deliveryTag")
  void shouldExcludeDeliveryTag() throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(new DefaultMessage("Tadaaaaa!"));
    assertThat(json).doesNotContain("deliveryTag");
  }

  @Test
  @DisplayName("Serialization should exclude field body")
  void shouldExcludeBody() throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(new DefaultMessage("Tadaaaaa!"));
    assertThat(json).doesNotContain("body");
  }

  @Test
  @DisplayName("Deserialization should ignore unknown fields")
  void shouldIgnoreUnknownFields() throws IOException {
    DefaultMessage message = new DefaultMessage("Tadaaaaa!");
    String json = objectMapper.writeValueAsString(message);
    json = json.substring(0, json.length() - 1) + ", \"stupidField\": 0}";
    DefaultMessage restored = objectMapper.readValue(json, DefaultMessage.class);
    assertThat(message.getId()).isEqualTo(restored.getId());
  }

  @Test
  @DisplayName("Should serialize Envelope.retries")
  void shouldSerializeRetries() throws JsonProcessingException {
    DefaultMessage message = new DefaultMessage("something happened");
    message.getEnvelope().setRetries(42);
    assertThat(objectMapper.writeValueAsString(message)).contains("42");
  }

  @Test
  @DisplayName("Should deserialize Envelope.retries")
  void shouldDeserializeRetries() throws IOException {
    DefaultMessage message = new DefaultMessage("something happened");
    message.getEnvelope().setRetries(42);
    Message deserialized =
        objectMapper.readValue(objectMapper.writeValueAsString(message), DefaultMessage.class);
    assertThat(deserialized.getEnvelope().getRetries()).isEqualTo(42);
  }
}
