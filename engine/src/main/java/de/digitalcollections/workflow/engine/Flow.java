package de.digitalcollections.workflow.engine;

import de.digitalcollections.workflow.engine.model.Job;
import de.digitalcollections.workflow.engine.model.Message;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Flow<R, W> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Flow.class);

  private String inputChannel;

  private String outputChannel;

  private Function<Message, R> reader;

  private Function<R, W> transformer;

  private Function<W, Message> writer;

  protected Flow(String inputChannel, String outputChannel, Function<Message, R> reader, Function<R, W> transformer, Function<W, Message> writer) {
    this.inputChannel = inputChannel;
    this.outputChannel = outputChannel;
    this.reader = reader;
    this.transformer = transformer;
    this.writer = writer;
  }

  public Message process(Message message) {
    Job job = new Job<>(message, reader, transformer, writer);
    if (reader != null) {
      job.read();
    }
    if (transformer != null) {
      job.transform();
    }
    if (writer != null) {
      job.write();
    }
    return job.getResult();
  }

  public String getInputChannel() {
    return inputChannel;
  }

  public String getOutputChannel() {
    return outputChannel;
  }

  public boolean hasOutputChannel() {
    return outputChannel != null;
  }

}
