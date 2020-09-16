package com.github.dbmdz.flusswerk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.exceptions.InvalidMessageException;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(
    classes = {FlusswerkPropertiesConfiguration.class, FlusswerkConfiguration.class})
@Import({MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
public class NoFlowTest {

  private final RabbitUtil rabbitUtil;

  private final RabbitMQ rabbitMQ;

  @Autowired
  public NoFlowTest(Engine engine, RoutingProperties routingProperties, RabbitMQ rabbitMQ) {
    this.rabbitMQ = rabbitMQ;
    this.rabbitUtil = new RabbitUtil(rabbitMQ, routingProperties);
  }

  @Test
  public void shouldSendMessageToRoute()
      throws IOException, InterruptedException, InvalidMessageException {
    Message message = new Message("123");
    rabbitMQ.route("default").send(message);
    Message received = rabbitUtil.waitAndAck("target.queue", Duration.ofMillis(50));
    assertThat(received).isEqualTo(message);
  }
}
