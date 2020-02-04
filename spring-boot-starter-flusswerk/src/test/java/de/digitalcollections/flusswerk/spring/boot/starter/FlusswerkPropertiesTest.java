package de.digitalcollections.flusswerk.spring.boot.starter;

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(properties.getProcessing())
        .hasFieldOrPropertyWithValue("maxRetries", 5)
        .hasFieldOrPropertyWithValue("threads", 5);
  }

  @Test
  @DisplayName("Values of FlusswerkProperties.Connection are all set")
  public void valuesOfConnection() {
    assertThat(properties.getConnection())
        .hasFieldOrPropertyWithValue("connectTo", "my.rabbit.example.com:5672")
        .hasFieldOrPropertyWithValue("virtualHost", "vh1")
        .hasFieldOrPropertyWithValue("username", "guest")
        .hasFieldOrPropertyWithValue("password", "guest");
  }

  @Test
  @DisplayName("Values of FlusswerkProperties.Routing are all set")
  public void valuesOfRouting() {
    assertThat(properties.getRouting())
        .hasFieldOrPropertyWithValue("exchange", "my.exchange")
        .hasFieldOrPropertyWithValue("readFrom", new String[] {"first", "second"})
        .hasFieldOrPropertyWithValue("writeTo", "defalt.queue.to.write.to");
  }
}
