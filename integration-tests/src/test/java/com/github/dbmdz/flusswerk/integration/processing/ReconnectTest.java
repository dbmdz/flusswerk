package com.github.dbmdz.flusswerk.integration.processing;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.config.FlusswerkConfiguration;
import com.github.dbmdz.flusswerk.framework.config.FlusswerkPropertiesConfiguration;
import com.github.dbmdz.flusswerk.framework.config.properties.RabbitMQProperties;
import com.github.dbmdz.flusswerk.framework.config.properties.RoutingProperties;
import com.github.dbmdz.flusswerk.framework.engine.Engine;
import com.github.dbmdz.flusswerk.framework.flow.FlowSpec;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.jackson.FlusswerkObjectMapper;
import com.github.dbmdz.flusswerk.framework.model.IncomingMessageType;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitConnection;
import com.github.dbmdz.flusswerk.framework.rabbitmq.RabbitMQ;
import com.github.dbmdz.flusswerk.integration.RabbitUtil;
import com.github.dbmdz.flusswerk.integration.TestMessage;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
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

  private final RabbitConnection rabbitConnection;

  private final FlusswerkObjectMapper flusswerkObjectMapper;

  @Autowired
  public ReconnectTest(
      Engine engine,
      RoutingProperties routingProperties,
      RabbitMQ rabbitMQ,
      RabbitConnection rabbitConnection,
      FlusswerkObjectMapper flusswerkObjectMapper) {
    this.engine = engine;
    rabbitUtil = new RabbitUtil(rabbitMQ, routingProperties);
    this.rabbitConnection = rabbitConnection;
    this.flusswerkObjectMapper = flusswerkObjectMapper;
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
                log.info("Processing " + msg);
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

  @AfterEach
  void stopEngine() throws IOException, InterruptedException {
    engine.stop();
    rabbitUtil.purgeQueues();
    proxy.delete();
  }

  @DisplayName("during startup, retry registration of consumers")
  @Test
  void recoverFromDisruptionDuringStartup() throws IOException, InterruptedException {
    TestMessage input = new TestMessage("hello world");
    log.info("Sending message");
    rabbitUtil.send(input);
    Thread toxicThread =
        new Thread(
            () -> {
              try {
                log.info("Disrupting RabbitMQ connection");
                proxy.toxics().timeout("timeout", ToxicDirection.DOWNSTREAM, 1000);
                Thread.sleep(15000);
                log.info("Re-establishing RabbitMQ connection");
                proxy.toxics().get("timeout").remove();
              } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
              }
            });
    Thread engineThread =
        new Thread(
            () -> {
              try {
                Thread.sleep(200);
                log.info("Starting Flusswerk engine");
                engine.start();
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            });

    engineThread.start();
    toxicThread.start();
    toxicThread.join();
    engineThread.join();

    Thread.sleep(15000);
    log.info("Collection should be recovered now, get processed message");
    Channel channel = rabbitConnection.getChannel();
    ReconnectTest.CollectMessages collectMessages =
        new CollectMessages(channel, flusswerkObjectMapper);
    channel.basicConsume("output", false, collectMessages);

    Thread.sleep(2000);
    channel.basicCancel(collectMessages.getConsumerTag());

    assertThat(collectMessages.getMessages())
        .map(TestMessage.class::cast)
        .map(TestMessage::getId)
        .contains("HELLO WORLD");
    assertThat(channel.consumerCount("input.first") + channel.consumerCount("input.second"))
        .isEqualTo(4L);
  }

  @DisplayName("processing should continue with the last message that wasn't acknowledged")
  @Test
  public void recoveryAfterDisruptionDuringProcessing() throws Exception {
    TestMessage input = new TestMessage("hello world");
    engine.start();

    log.info("Sending message");
    rabbitUtil.send(input);
    log.info("Disrupting RabbitMQ connection");
    // Stop letting packets through and close the connection after 1sec
    proxy.toxics().timeout("timeout", ToxicDirection.DOWNSTREAM, 1000);
    // Leave the engine alone for 15secs
    Thread.sleep(15000);
    log.info("Re-establishing RabbitMQ connection");
    proxy.toxics().get("timeout").remove();
    Thread.sleep(15000);
    log.info("Connection should be recovered now, sending another message");
    rabbitUtil.send(new TestMessage("and now again"));

    Channel channel = rabbitConnection.getChannel();
    CollectMessages collectMessages = new CollectMessages(channel, flusswerkObjectMapper);
    channel.basicConsume("output", false, collectMessages);

    // Give the second message some time to be processed
    Thread.sleep(2000);

    channel.basicCancel(collectMessages.getConsumerTag());

    assertThat(collectMessages.getMessages())
        .map(TestMessage.class::cast)
        .map(TestMessage::getId)
        .contains("HELLO WORLD", "AND NOW AGAIN");
  }

  static class CollectMessages extends DefaultConsumer {
    private final List<Message> messages = new ArrayList<>();
    private final FlusswerkObjectMapper flusswerkObjectMapper;
    private final Channel channel;

    public CollectMessages(Channel channel, FlusswerkObjectMapper flusswerkObjectMapper) {
      super(channel);
      this.channel = channel;
      this.flusswerkObjectMapper = flusswerkObjectMapper;
    }

    public void handleDelivery(
        String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
        throws IOException {
      String json = new String(body, StandardCharsets.UTF_8);
      Message message = flusswerkObjectMapper.deserialize(json);
      messages.add(message);
      channel.basicAck(envelope.getDeliveryTag(), false);
    }

    public List<Message> getMessages() {
      return messages;
    }
  }
}
