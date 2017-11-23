package de.digitalcollections.workflow.engine;

import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static de.digitalcollections.workflow.engine.model.DefaultMessage.withType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EngineTest {

  private static final String IN = "in";

  private static final String OUT = "out";

  private static final String EXCHANGE = "exchange";

  private static final String DLX = "dlx.exchange";

  private Message[] moreMessages(int number) {
    Message[] messages = new Message[number];
    for (int i = 0; i < messages.length; i++) {
      messages[i] =  DefaultMessage.withType("White Room");
    }
    return messages;
  }

  @Disabled("Test does not work in Travis CI")
  @Test
  public void engineShouldUseMaxNumberOfWorkers() throws IOException, InterruptedException {
    MessageBroker messageBroker = mock(MessageBroker.class);
    when(messageBroker.receive(any())).thenReturn(DefaultMessage.withType("White Room"));

    Semaphore semaphore = new Semaphore(1);
    semaphore.drainPermits();

    Flow<String, String> flow = new Flow<>(
        IN, OUT,
        Message::getType,
        s -> {
          try {
            semaphore.acquire(); // Block this worker to count it only once
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          return s;
        },
        DefaultMessage::withType
    );

    Engine engine = new Engine(messageBroker, flow);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(engine::start);

    int millisecondsWaited = 0;
    while (engine.getAvailableWorkers() > 0 && millisecondsWaited < 1000) {
      millisecondsWaited += 10;
      TimeUnit.MILLISECONDS.sleep(10);
    }


    assertThat(engine.getActiveWorkers()).isEqualTo(engine.getConcurrentWorkers());
  }

  @Disabled("Test results are sometimes wrong")
  @Test
  public void engineShouldSendMessageToOut() throws IOException, InterruptedException {
    MessageBroker messageBroker = mock(MessageBroker.class);
    when(messageBroker.receive(any())).thenReturn(withType("White Room"));

    AtomicInteger messagesSent = new AtomicInteger();

    Flow<String, String> flow = new Flow<>(
        IN, OUT,
        Message::getType,
        s -> {
          messagesSent.incrementAndGet();
          return s;
        },
        DefaultMessage::withType
    );

    Engine engine = new Engine(messageBroker, flow);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(engine::start);

    int millisecondsWaited = 0;
    while (messagesSent.get() == 0 && millisecondsWaited < 250) {
      millisecondsWaited += 1;
      TimeUnit.MILLISECONDS.sleep(1);
    }

    ArgumentCaptor<String> routingCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
    verify(messageBroker, atLeastOnce()).send(routingCaptor.capture(), messageCaptor.capture());

    assertThat(routingCaptor.getValue()).isEqualTo("out");
    assertThat(messageCaptor.getValue().getType()).isEqualTo("White Room");
    System.out.println(messagesSent.get());
  }


}
