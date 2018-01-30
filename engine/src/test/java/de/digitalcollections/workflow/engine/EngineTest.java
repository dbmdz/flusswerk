package de.digitalcollections.workflow.engine;

import de.digitalcollections.workflow.engine.exceptions.FinallyFailedProcessException;
import de.digitalcollections.workflow.engine.exceptions.RetriableProcessException;
import de.digitalcollections.workflow.engine.flow.Flow;
import de.digitalcollections.workflow.engine.flow.FlowBuilder;
import de.digitalcollections.workflow.engine.messagebroker.MessageBroker;
import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EngineTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(EngineTest.class);

  private static final String IN = "in";

  private static final String OUT = "out";

  private static final String EXCHANGE = "exchange";

  private static final String DLX = "exchange.retry";
  private MessageBroker messageBroker;
  private Flow<DefaultMessage, String, String> flowWithoutProblems;

  private Message[] moreMessages(int number) {
    Message[] messages = new Message[number];
    for (int i = 0; i < messages.length; i++) {
      messages[i] = new DefaultMessage("White Room");
    }
    return messages;
  }

  @BeforeEach
  void setUp() {
    messageBroker = mock(MessageBroker.class);
    flowWithoutProblems = new FlowBuilder<DefaultMessage, String, String>()
        .read(READ_SOME_STRING)
        .transform(Function.identity())
        .write(WRITE_SOME_STRING)
        .build();
  }

  @Test
  public void engineShouldUseMaxNumberOfWorkers() throws IOException, InterruptedException {
    when(messageBroker.receive()).thenReturn(new DefaultMessage("White Room"));

    Semaphore semaphore = new Semaphore(1);
    semaphore.drainPermits();

    Flow flow = new FlowBuilder<DefaultMessage, String, String>()
        .read(DefaultMessage::getId)
        .transform(s -> {
          try {
            LOGGER.debug("Trying to acquire semaphore, should block (Thread id {})", Thread.currentThread().getId());
            semaphore.acquire(); // Block this worker to count it only once
            LOGGER.debug("Got semaphore (Thread id {})", Thread.currentThread().getId());
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          return s;
        })
        .write((Function<String, Message>) DefaultMessage::new)
        .build();

    Engine engine = new Engine(messageBroker, flow);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(engine::start);

    int millisecondsWaited = 0;
    EngineStats engineStats = engine.getStats();
    boolean shouldWait = engineStats.getAvailableWorkers() > 0;
    while (shouldWait) {
      LOGGER.info("Waiting for workers to get busy: {} active, {} free after {} ms", engineStats.getActiveWorkers(), engineStats.getAvailableWorkers(), millisecondsWaited);
      millisecondsWaited += 100;
      TimeUnit.MILLISECONDS.sleep(100);
      engineStats = engine.getStats();
      shouldWait = (engineStats.getAvailableWorkers() > 0) && (millisecondsWaited < 300000);
    }

    assertThat(engineStats.getActiveWorkers()).as("There were %d workers expected, but only %d running after waiting for %d ms",
        engineStats.getConcurrentWorkers(), engineStats.getActiveWorkers(), millisecondsWaited)
        .isEqualTo(engineStats.getConcurrentWorkers());

    engine.stop();
  }

//  @Test
//  public void engineShouldSendMessageToOut() throws IOException, InterruptedException {
//    when(messageBroker.receive(any())).thenReturn(withType("White Room"));
//
//    AtomicInteger messagesSent = new AtomicInteger();
//
//    Flow<Message, String, String> flow = new FlowBuilder<Message, String, String>()
//        .read(Message::getType)
//        .transform(s -> {
//          messagesSent.incrementAndGet();
//          return s;
//        })
//        .write(DefaultMessage::withType)
//        .build();
//
//    Engine engine = new Engine(messageBroker, flow);
//    ExecutorService executorService = Executors.newSingleThreadExecutor();
//    executorService.submit(engine::start);
//
//    int millisecondsWaited = 0;
//    while (messagesSent.get() == 0 && millisecondsWaited < 250) {
//      millisecondsWaited += 1;
//      TimeUnit.MILLISECONDS.sleep(1);
//    }
//
//    ArgumentCaptor<String> routingCaptor = ArgumentCaptor.forClass(String.class);
//    ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
//    verify(messageBroker, atLeastOnce()).send(routingCaptor.capture(), messageCaptor.capture());
//
//    assertThat(routingCaptor.getValue()).isEqualTo("out");
//    assertThat(messageCaptor.getValue().getType()).isEqualTo("White Room");
//    System.out.println(messagesSent.get());
//  }

  private final Function<DefaultMessage, String> READ_SOME_STRING = DefaultMessage::getId;

  private final Function<String, Message> WRITE_SOME_STRING = DefaultMessage::new;

  @Test
  @DisplayName("Engine should reject a message failing processing")
  void processShouldRejectMessageOnFailure() throws IOException {
    Flow flow = new FlowBuilder<DefaultMessage, String, String>()
        .read(READ_SOME_STRING)
        .transform(s -> {
          throw new RuntimeException("Aaaaaaah!");
        })
        .write(WRITE_SOME_STRING)
        .build();

    Engine engine = new Engine(messageBroker, flow);
    Message message = new DefaultMessage();

    engine.process(message);

    verify(messageBroker).reject(message);
    verify(messageBroker, never()).ack(message);
  }

  @Test
  @DisplayName("Engine should accept a message processed without failure")
  void processShouldAcceptMessageWithoutFailure() throws IOException {
    Engine engine = new Engine(messageBroker, flowWithoutProblems);
    Message message = new DefaultMessage();
    engine.process(message);

    verify(messageBroker).ack(message);
    verify(messageBroker, never()).reject(message);
  }


  @Test
  @DisplayName("Engine should send a message")
  void processShouldSendMessage() throws IOException {
    Engine engine = new Engine(messageBroker, flowWithoutProblems);
    engine.process(new DefaultMessage());

    verify(messageBroker).send(any(Message.class));
  }

  @Test
  @DisplayName("RetriableProcessException shall reject message temporarily")
  void retriableProcessExceptionShallRejectTemporarily() throws IOException {
    Flow flow = new FlowBuilder<DefaultMessage, String, String>()
        .read(READ_SOME_STRING)
        .transform(s -> {
          throw new RetriableProcessException("Try again after a cup of coffee");
        })
        .write(WRITE_SOME_STRING)
        .build();

    Engine engine = new Engine(messageBroker, flow);
    Message message = new DefaultMessage();

    engine.process(message);

    verify(messageBroker).reject(message);
  }

  @Test
  @DisplayName("FinallyFailedProcessException shall fail message")
  void finallyFailedProcessExceptionShallFailMessage() throws IOException {
    Flow flow = new FlowBuilder<DefaultMessage, String, String>()
        .read(READ_SOME_STRING)
        .transform(s -> {
          throw new FinallyFailedProcessException("Never again!");
        })
        .write(WRITE_SOME_STRING)
        .build();

    Engine engine = new Engine(messageBroker, flow);
    Message message = new DefaultMessage();

    engine.process(message);

    verify(messageBroker).fail(message);
  }

}
