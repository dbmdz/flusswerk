package org.mdz.dzp.workflow.neo.engine;

import org.junit.Before;
import org.junit.Test;
import org.mdz.dzp.workflow.neo.engine.model.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;

public class FlowTest {

  private Flow<String, String> flow;

  @Before
  public void setUp() {
    flow = new Flow<>();
  }

  @Test(expected = IllegalStateException.class)
  public void startShouldFailIfReaderIsMissing() throws Exception {
    flow.transformer(x -> "Stuff")
        .writer(x -> new Message("Other Stuff"))
        .start();
  }

  @Test(expected = IllegalStateException.class)
  public void startShouldFailIfTransformerIsMissing() throws Exception {
    flow.reader(message -> "Stuff")
        .writer(x -> new Message("Other Stuff"))
        .start();
  }

  @Test(expected = IllegalStateException.class)
  public void startShouldFailIfWriterIsMissing() throws Exception {
    flow.reader(message -> "Stuff")
        .transformer(x -> x)
        .start();
  }

  @Test
  public void processShouldTriggerRead() throws Exception {
    StoreArgumentFunction<Message, String> reader = new StoreArgumentFunction<>();

    flow.reader(reader)
        .transformer(x -> x)
        .writer(x -> new Message("Other stuff"))
        .start();
    flow.process(new Message("Hey"));

    assertThat(reader.getValue()).returns("Hey", from(Message::getValue));
  }

  @Test
  public void processShouldTriggerTransform() throws Exception {
    StoreArgumentFunction<String, String> transformer = new StoreArgumentFunction<>();

    flow.reader(message -> "Stuff")
        .transformer(transformer)
        .writer(x -> new Message("Other stuff"))
        .start();
    flow.process(new Message("Hey"));

    assertThat(transformer.getValue()).isEqualTo("Stuff");
  }

  @Test
  public void processShouldTriggerWrite() throws Exception {
    StoreArgumentFunction<String, Message> writer = new StoreArgumentFunction<>();
    flow.reader(message -> "Stuff")
        .transformer(x -> "Stuff to write")
        .writer(writer)
        .start();
    flow.process(new Message("Hey"));
    assertThat(writer.getValue()).isEqualTo("Stuff to write");
  }

}
