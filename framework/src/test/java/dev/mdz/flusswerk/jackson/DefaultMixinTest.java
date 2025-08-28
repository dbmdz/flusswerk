package dev.mdz.flusswerk.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.mdz.flusswerk.TestMessage;
import dev.mdz.flusswerk.model.IncomingMessageType;
import dev.mdz.flusswerk.model.Message;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultMixinTest {

  private FlusswerkObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new FlusswerkObjectMapper(new IncomingMessageType());
  }

  @Test
  @DisplayName("Deserialization should ignore unknown fields")
  void shouldIgnoreUnknownFields() throws IOException {
    Message message = new Message();
    String json = objectMapper.writeValueAsString(message);
    json = json.substring(0, json.length() - 1) + ", \"stupidField\": 0}";
    Message restored = objectMapper.readValue(json, Message.class);
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
    Message message = new Message();
    message.getEnvelope().setRetries(42);
    Message deserialized =
        objectMapper.readValue(objectMapper.writeValueAsString(message), Message.class);
    assertThat(deserialized.getEnvelope().getRetries()).isEqualTo(42);
  }
}
