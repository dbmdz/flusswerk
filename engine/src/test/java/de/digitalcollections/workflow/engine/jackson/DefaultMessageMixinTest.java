package de.digitalcollections.workflow.engine.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;
import de.digitalcollections.workflow.engine.model.Meta;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultMessageMixinTest {

  private class SpecialMessage extends DefaultMessage {
    String specialField;
    public SpecialMessage() {
      super();
    }
    public SpecialMessage(String specialField) {
      this.specialField = specialField;
    }

    public String getSpecialField() {
      return specialField;
    }
    public void setSpecialField(String specialField) {
      this.specialField = specialField;
    }
  }

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.addMixIn(Message.class, DefaultMessageMixin.class);
    objectMapper.addMixIn(Meta.class, MetaMixin.class);
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.enable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
  }

  @Test
  @DisplayName("Serialization should exclude field deliveryTag")
  void shouldExcludeDeliveryTag() throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(DefaultMessage.withType("Tadaaaaa!"));
    assertThat(json).doesNotContain("deliveryTag");
  }

  @Test
  @DisplayName("Serialization should exclude field body")
  void shouldExcludeBody() throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(DefaultMessage.withType("Tadaaaaa!"));
    assertThat(json).doesNotContain("body");
  }

  @Test
  @DisplayName("Deserialization should ignore unknown fields")
  void shouldIgnoreUnknownFields() throws IOException {
    DefaultMessage message = DefaultMessage.withType("Tadaaaaa!").andId("X0000012-3");
    String json = objectMapper.writeValueAsString(message);
    json = json.substring(0, json.length() - 1) + ", \"stupidField\": 0}";
    System.out.println(json);
    DefaultMessage restored = objectMapper.readValue(json, DefaultMessage.class);
    assertThat(message.getType()).isEqualTo(restored.getType());
  }


//  @Test
//  @DisplayName("Serialization should preserve parameters")
//  void shouldPreserveParams() throws IOException {
//    Message message = new Message();
//    message.put("floob", "gooobl");
//    message.put("bingle", "bongle");
//    Message restored = objectMapper.readValue(objectMapper.writeValueAsString(message), Message.class);
//    assertThat(restored.getData()).isEqualTo(message.getData());
//  }

//  @Test
//  @DisplayName("Serialization should work for subtypes")
//  void shouldSaveSpecialMessage() throws IOException {
//    SpecialMessage message = new SpecialMessage("by the hammer of Thor");
//    SpecialMessage restored = objectMapper.readValue(objectMapper.writeValueAsString(message), SpecialMessage.class);
//    assertThat(restored.getSpecialField()).isEqualTo(message.getSpecialField());
//  }

}
