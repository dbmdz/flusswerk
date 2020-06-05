package com.github.dbmdz.flusswerk.framework.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.dbmdz.flusswerk.framework.TestMessage;
import com.github.dbmdz.flusswerk.framework.model.Envelope;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultMixinTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper =
        new ObjectMapper()
            .addMixIn(Message.class, DefaultMixin.class)
            .addMixIn(Envelope.class, EnvelopeMixin.class)
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
  }

  @Test
  @DisplayName("Deserialization should ignore unknown fields")
  void shouldIgnoreUnknownFields() throws IOException {
    Message message = new Message("tracing-123");
    String json = objectMapper.writeValueAsString(message);
    json = json.substring(0, json.length() - 1) + ", \"stupidField\": 0}";
    Message restored = objectMapper.readValue(json, Message.class);
    assertThat(message.getTracingId()).isEqualTo(restored.getTracingId());
  }

  @Test
  @DisplayName("Should serialize Envelope.retries")
  void shouldSerializeRetries() throws JsonProcessingException {
    TestMessage message = new TestMessage("abc123", "flow-3000");
    message.getEnvelope().setRetries(42);
    assertThat(objectMapper.writeValueAsString(message)).contains("42");
  }

  @Test
  @DisplayName("Should deserialize Envelope.retries")
  void shouldDeserializeRetries() throws IOException {
    Message message = new Message("tracing-123");
    message.getEnvelope().setRetries(42);
    Message deserialized =
        objectMapper.readValue(objectMapper.writeValueAsString(message), Message.class);
    assertThat(deserialized.getEnvelope().getRetries()).isEqualTo(42);
  }
}
