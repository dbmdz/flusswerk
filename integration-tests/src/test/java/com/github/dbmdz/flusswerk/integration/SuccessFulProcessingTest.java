package com.github.dbmdz.flusswerk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dbmdz.flusswerk.framework.Engine;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.flow.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.messagebroker.MessageBroker;
import com.github.dbmdz.flusswerk.framework.model.DefaultMessage;
import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SuccessFulProcessingTest {

  private static final String QUEUE_IN = "test.in";
  private static final String QUEUE_OUT = "test.out";
  private static final String QUEUE_FAILED = "test.in.failed";
  private static final String QUEUE_RETRY = "test.in.retry";

  private Backend backend;

  @BeforeEach
  public void setUp() {
    backend = new Backend(QUEUE_IN, QUEUE_OUT);
  }

  @Test
  public void successfulMessagesShouldGoToOutQueue() throws Exception {
    MessageBroker messageBroker = backend.getMessageBroker();
    Flow flow =
        new FlowBuilder<DefaultMessage, DefaultMessage, DefaultMessage>()
            .read(m -> m)
            .transform(m -> m)
            .writeAndSend((DefaultMessage m) -> m)
            .build();

    Engine engine = new Engine(messageBroker, flow);

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(engine::start);
    messageBroker.send(QUEUE_IN, new DefaultMessage("123456"));

    Message message = backend.waitForMessageFrom(QUEUE_OUT, 10_000);
    assertThat(message.getId()).isEqualTo("123456");
    assertThatMessageFrom(QUEUE_IN).isNull();
    assertThatMessageFrom(QUEUE_FAILED).isNull();
    assertThatMessageFrom(QUEUE_RETRY).isNull();

    engine.stop();
    executorService.shutdownNow();
  }

  private ObjectAssert<? extends Message<?>> assertThatMessageFrom(String name) throws Exception {
    return assertThat(backend.waitForMessageFrom(QUEUE_FAILED, 1000));
  }
}
