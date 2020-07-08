package com.github.dbmdz.flusswerk.framework.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = FlusswerkPropertiesConfiguration.class)
public class FlusswerkPropertiesTest {

  @Autowired private FlusswerkProperties properties;

  @Test
  @DisplayName("Values of FlusswerkProperties.Processing are all set")
  public void valuesOfProcessing() {
    assertThat(properties.getProcessing()).hasFieldOrPropertyWithValue("threads", 5);
  }

  @Test
  @DisplayName("Values of FlusswerkProperties.Connection are all set")
  public void valuesOfConnection() {
    assertThat(properties.getRabbitMQ())
        .hasFieldOrPropertyWithValue("hosts", List.of("my.rabbit.example.com"))
        .hasFieldOrPropertyWithValue("virtualHost", Optional.of("vh1"))
        .hasFieldOrPropertyWithValue("username", "guest")
        .hasFieldOrPropertyWithValue("password", "guest");
  }

  @Test
  @DisplayName("Values of FlusswerkProperties.Routing are all set")
  public void valuesOfRouting() {
    RoutingProperties routing = properties.getRouting();
    assertThat(routing)
        .hasFieldOrPropertyWithValue("exchange", "my.exchange")
        .hasFieldOrPropertyWithValue("incoming", List.of("first", "second"))
        .hasFieldOrPropertyWithValue("outgoing", Map.of("default", "default.queue.to.write.to"));

    assertThat(routing.getFailurePolicy("first"))
        .hasFieldOrPropertyWithValue("backoff", Duration.ofSeconds(15))
        .hasFieldOrPropertyWithValue("retries", 77)
        .hasFieldOrPropertyWithValue("retryRoutingKey", "first.custom.retry")
        .hasFieldOrPropertyWithValue("failedRoutingKey", "first.custom.failed");
  }
}
