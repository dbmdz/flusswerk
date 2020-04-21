package com.github.dbmdz.flusswerk.framework.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultMessageTest {

  @Test
  void putCanBeChained() {
    DefaultMessage message =
        new DefaultMessage("sundry").put("first", "is the first").put("second", "is the second");
    assertThat(message.get("first")).isEqualTo("is the first");
    assertThat(message.get("second")).isEqualTo("is the second");
  }

  @Test
  void putWorksWithNull() {
    DefaultMessage message = new DefaultMessage("hurz").put("key1", "value1").put(null);
    assertThat(message.get("key1")).isEqualTo("value1");
  }

  @Test
  void putAppends() {
    Map<String, String> map = new HashMap<>();
    map.put("key2", "value2");
    DefaultMessage message = new DefaultMessage("hurz").put("key1", "value1").put(map);
    assertThat(message.get("key1")).isEqualTo("value1");
    assertThat(message.get("key2")).isEqualTo("value2");
    assertThat(message.getData().keySet().size()).isEqualTo(2);
  }

  @Test
  void putOverwrites() {
    Map<String, String> map = new HashMap<>();
    map.put("key1", "value2");
    DefaultMessage message = new DefaultMessage("hurz").put("key1", "value1").put(map);
    assertThat(message.get("key1")).isEqualTo("value2");
    assertThat(message.getData().keySet().size()).isEqualTo(1);
  }
}
