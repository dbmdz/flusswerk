package org.mdz.dzp.workflow.neo.engine.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mdz.dzp.workflow.neo.engine.model.DefaultMessage;
import org.mdz.dzp.workflow.neo.engine.model.Message;

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

//  @Test
//  @DisplayName("Serialization should preserve parameters")
//  void shouldPreserveParams() throws IOException {
//    Message message = new Message();
//    message.put("floob", "gooobl");
//    message.put("bingle", "bongle");
//    Message restored = objectMapper.readValue(objectMapper.writeValueAsString(message), Message.class);
//    assertThat(restored.getParameters()).isEqualTo(message.getParameters());
//  }

//  @Test
//  @DisplayName("Serialization should work for subtypes")
//  void shouldSaveSpecialMessage() throws IOException {
//    SpecialMessage message = new SpecialMessage("by the hammer of Thor");
//    SpecialMessage restored = objectMapper.readValue(objectMapper.writeValueAsString(message), SpecialMessage.class);
//    assertThat(restored.getSpecialField()).isEqualTo(message.getSpecialField());
//  }

}
