package org.mdz.dzp.workflow.neo.engine.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mdz.dzp.workflow.neo.engine.model.Message;

import static org.assertj.core.api.Assertions.assertThat;

class MessageMixinTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.addMixIn(Message.class, MessageMixin.class);
  }

  @Test
  @DisplayName("Serialization should exclude field retries")
  void shouldExcludeRetries() throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(new Message("Tadaaaaa!"));
    assertThat(json).doesNotContain("retries");
  }

  @Test
  @DisplayName("Serialization should exclude field deliveryTag")
  void shouldExcludeDeliveryTag() throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(new Message("Tadaaaaa!"));
    assertThat(json).doesNotContain("deliveryTag");
  }

  @Test
  @DisplayName("Serialization should exclude field body")
  void shouldExcludeBody() throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(new Message("Tadaaaaa!"));
    assertThat(json).doesNotContain("body");
  }


}
