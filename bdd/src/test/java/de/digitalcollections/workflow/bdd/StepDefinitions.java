package de.digitalcollections.workflow.bdd;

import cucumber.api.java8.En;
import de.digitalcollections.workflow.engine.flow.Flow;
import de.digitalcollections.workflow.engine.flow.FlowBuilder;
import de.digitalcollections.workflow.engine.messagebroker.MessageBroker;
import de.digitalcollections.workflow.engine.messagebroker.MessageBrokerBuilder;
import de.digitalcollections.workflow.engine.model.DefaultMessage;
import de.digitalcollections.workflow.engine.model.Message;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;


public class StepDefinitions implements En {

  private static final Logger LOGGER = LoggerFactory.getLogger(StepDefinitions.class);

  private Orchestration orchestration = Orchestration.getInstance();

  private MessageBroker messageBroker;

  private List<Message> messagesToSend;

  private String queueToSendTo;


  public StepDefinitions() {
    Given("I have an message broker with default config and a message in ([\\w\\.]+)", (String queue) -> {
      messageBroker = orchestration.createMessageBroker(
          new MessageBrokerBuilder()
              .deadLetterWait(20)
              .readFrom("bdd.in")
              .writeTo("bdd.out")
      );
      messagesToSend = Collections.singletonList(DefaultMessage.withId("happy message"));
      queueToSendTo = queue;
    });

    When("the processing always fails", () -> {
      Flow flow = new FlowBuilder<>()
          .read(Message::toString)
          .transform(s -> {
            throw new RuntimeException("Fail!");
          })
          .write(s -> DefaultMessage.withId(s.toString()))
          .build();

      // Preparation finished, start everything
      start(flow);
    });

    When("^the processing always works$", () -> {
      Flow flow = new FlowBuilder<DefaultMessage, String, String>()
          .read(DefaultMessage::getId)
          .transform(s -> s)
          .write(s -> DefaultMessage.withId(s).put("blah", "blubb"))
          .build();

      // Preparation finished, start everything
      start(flow);
    });

    Then("the message in queue ([\\w\\.]+) has (\\d+) retries", (String queue, Integer retries) -> {
      Message<?> message = waitForMessageFrom(queue);
      messageBroker.ack(message);
      assertThat(message.getEnvelope().getRetries()).isEqualTo(5);
    });

    Then("^the message in queue ([\\w\\.]+) has a field ([\\w\\.]+) with value ([\\w\\.]+)$", (String queue, String field, String value) -> {
      DefaultMessage message = (DefaultMessage) waitForMessageFrom(queue);
      messageBroker.ack(message);
      assertThat(message.get(field)).isEqualTo(value);
    });

    Then("([\\w\\.]+) is empty", (String queue) -> {
      assertThat(messageBroker.receive(queue)).isNull();
    });

  }

  private Message<?> waitForMessageFrom(String queue) throws IOException, InterruptedException {
    Message<?> message = null;
    long time = System.currentTimeMillis();
    long timeout = 30 * 1000;

    while (message == null && (System.currentTimeMillis() - time < timeout)) {
      message = messageBroker.receive(queue);
      if (message == null) {
        TimeUnit.MILLISECONDS.sleep(50);
      }
    }

    assertThat(message).withFailMessage("There was no message in " + queue).isNotNull();

    return message;
  }

  private void start(Flow<?, ?, ?> flow) throws IOException, InterruptedException {
    orchestration.startEngine(messageBroker, flow);
    messageBroker.send(queueToSendTo, messagesToSend);
  }

}
