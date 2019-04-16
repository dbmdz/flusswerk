package de.digitalcollections.flusswerk.engine.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.digitalcollections.flusswerk.engine.model.FlowMessage;
import de.digitalcollections.flusswerk.engine.model.Envelope;
import de.digitalcollections.flusswerk.engine.model.Message;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlowMessageMixinTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper()
        .addMixIn(Message.class, FlowMessageMixin.class)
        .addMixIn(Envelope.class, EnvelopeMixin.class)
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
  }

  @Test
  @DisplayName("Deserialization should ignore unknown fields")
  void shouldIgnoreUnknownFields() throws IOException {
    FlowMessage message = new FlowMessage("abc123", "flow-3000");
    String json = objectMapper.writeValueAsString(message);
    json = json.substring(0, json.length() - 1) + ", \"stupidField\": 0}";
    FlowMessage restored = objectMapper.readValue(json, FlowMessage.class);
    assertThat(message.getId()).isEqualTo(restored.getId());
  }

  @Test
  @DisplayName("Should serialize Envelope.retries")
  void shouldSerializeRetries() throws JsonProcessingException {
    FlowMessage message = new FlowMessage("abc123", "flow-3000");
    message.getEnvelope().setRetries(42);
    assertThat(objectMapper.writeValueAsString(message)).contains("42");
  }

  @Test
  @DisplayName("Should deserialize Envelope.retries")
  void shouldDeserializeRetries() throws IOException {
    FlowMessage message = new FlowMessage("abc123", "flow-3000");
    message.getEnvelope().setRetries(42);
    Message deserialized = objectMapper.readValue(objectMapper.writeValueAsString(message), FlowMessage.class);
    assertThat(deserialized.getEnvelope().getRetries()).isEqualTo(42);
  }

  @Test
  @DisplayName("Should serialize and deserialize arbitrary values")
  void shouldSerializeAndDeserializeArbitraryValues() throws IOException {
    FlowMessage message = new FlowMessage("abc123", "flow-3000");
    message.put("purpose of life", "42");
    String json = objectMapper.writeValueAsString(message);
    FlowMessage deserialized = objectMapper.readValue(json, FlowMessage.class);
    assertThat(deserialized.get("purpose of life")).isEqualTo("42");
  }

}
