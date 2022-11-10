package com.github.dbmdz.flusswerk.integration.processing;

import static com.github.dbmdz.flusswerk.integration.RabbitUtilAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import com.github.dbmdz.flusswerk.framework.config.properties.RabbitMQProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitConnection;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import com.github.dbmdz.flusswerk.integration.RabbitUtil;
import com.github.dbmdz.flusswerk.integration.TestMessage;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ContextConfiguration(
    classes = {
      FlusswerkPropertiesConfiguration.class,
      FlusswerkConfiguration.class,
      ReconnectTest.FlowConfiguration.class
    })
@Import({MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@DisplayName("When the RabbitMQ connection is lost")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
public class ReconnectTest {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule static final Network network = Network.newNetwork();

  @Container
  static final RabbitMQContainer rabbitMQContainer =
      new RabbitMQContainer("rabbitmq:3-management-alpine")
          .withNetwork(network)
          .withExposedPorts(5672)
          .withNetworkAliases("rabbitmq");

  @Rule
  static final ToxiproxyContainer toxiProxy =
      new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
          .withExposedPorts(5672, 8474)
          .withNetwork(network);

  static Proxy proxy;

  private final Engine engine;

  private final RabbitUtil rabbitUtil;

  @Autowired
  public ReconnectTest(Engine engine, RoutingProperties routingProperties, RabbitMQ rabbitMQ) {
    this.engine = engine;
    rabbitUtil = new RabbitUtil(rabbitMQ, routingProperties);
  }

  @TestConfiguration
  static class FlowConfiguration {
    @Bean
    @Primary
    public RabbitConnection rabbitConnection() throws IOException {
      // Need to have the containers started go get the host and port mappings
      rabbitMQContainer.start();
      toxiProxy.start();
      final ToxiproxyClient toxiProxyClient =
          new ToxiproxyClient(toxiProxy.getHost(), toxiProxy.getControlPort());
      proxy = toxiProxyClient.createProxy("rabbitmq", "0.0.0.0:5672", "rabbitmq:5672");
      return new RabbitConnection(
          new RabbitMQProperties(
              List.of(String.format("%s:%d", toxiProxy.getHost(), toxiProxy.getMappedPort(5672))),
              "/",
              "guest",
              "guest"),
          "reconnect-test");
    }

    @Bean
    public IncomingMessageType incomingMessageType() {
      return new IncomingMessageType(TestMessage.class);
    }

    @Bean
    public FlowSpec flowSpec() {
      return FlowBuilder.flow(TestMessage.class, String.class, String.class)
          .reader(
              msg -> {
                try {
                  Thread.sleep(100);
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
                return msg.getId();
              })
          .transformer(
              msg -> {
                try {
                  Thread.sleep(100);
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
                return msg.toUpperCase();
              })
          .writerSendingMessage(
              msg -> {
                try {
                  // delay: disruption will occur before message processing is completed
                  Thread.sleep(1000);
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
                return new TestMessage(msg);
              })
          .build();
    }
  }

  @BeforeEach
  void startEngine() {
    engine.start();
  }

  @AfterEach
  void stopEngine() throws IOException {
    engine.stop();
    rabbitUtil.purgeQueues();
  }

  @DisplayName("processing should continue with the last message that wasn't acknowledged")
  @Test
  public void recoveryAfterDisruptionDuringProcessing() throws Exception {
    TestMessage input = new TestMessage("hello world");

    log.info("Sending message");
    rabbitUtil.send(input);
    log.info("Disrupting RabbitMQ connection");
    // Stop letting packets through and close the connection after 1sec
    proxy.toxics().timeout("timeout", ToxicDirection.DOWNSTREAM, 1000);
    // Leave the engine alone for 15secs
    Thread.sleep(15000);
    log.info("Re-establishing RabbitMQ connection");
    proxy.toxics().get("timeout").remove();
    log.info("Checking that connection has recovered by fetching a message ");
    // When using Basic.Get the RabbitMQ client will not adapt delivery tags by adding an offset.
    // When acknowledging the message, however, the offset will be subtracted so that we end up with
    // invalid delivery tags. (see
    // https://github.com/rabbitmq/rabbitmq-java-client/blob/main/src/main/java/com/rabbitmq/client/impl/recovery/RecoveryAwareChannelN.java)
    // Solution: use automatic acknowledgements with Basic.Get.
    var received = (TestMessage) rabbitUtil.receive(true);
    assertThat(received.getId()).isEqualTo("HELLO WORLD");
    // initial message will be retransmitted because of missing ACK
    received = (TestMessage) rabbitUtil.receive(true);
    assertThat(received.getId()).isEqualTo("HELLO WORLD");
    assertThat(rabbitUtil).allQueuesAreEmpty();

    // Any subsequent  messages should go through without any additional delay
    input = new TestMessage("and now again");
    rabbitUtil.send(input);
    received = (TestMessage) rabbitUtil.receive(true);
    assertThat(received.getId()).isEqualTo("AND NOW AGAIN");
    assertThat(rabbitUtil).allQueuesAreEmpty();
  }
}
