package com.github.dbmdz.flusswerk.framework.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.dbmdz.flusswerk.framework.jackson.FlusswerkObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnvelopeTest {

  private FlusswerkObjectMapper flusswerkObjectMapper;

  @BeforeEach
  void setUp() {
    flusswerkObjectMapper = new FlusswerkObjectMapper(new IncomingMessageType());
  }

  @DisplayName("Deserialize should ignore timestamp (compatibility)")
  @Test
  void deserializeShouldIgnoreTimestamp() throws JsonProcessingException {
    String json = "{\"timestamp\": [2021, 10, 10, 16, 23, 11]}";
    flusswerkObjectMapper.readValue(json, Envelope.class); // should not throw an exception
  }
}
