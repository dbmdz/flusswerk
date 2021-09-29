package com.github.dbmdz.flusswerk.framework.fixtures;

import com.github.dbmdz.flusswerk.framework.TestMessage;
import com.github.dbmdz.flusswerk.framework.flow.Flow;
import com.github.dbmdz.flusswerk.framework.flow.builder.FlowBuilder;
import com.github.dbmdz.flusswerk.framework.locking.NoOpLockManager;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.util.function.Function;

public class Flows {

  public static Flow passthroughFlow() {
    return messageProcessor(m -> m);
  }

  public static Flow consumingFlow() {
    var spec = FlowBuilder.messageProcessor(Message.class).consume(m -> {}).build();
    return new Flow(spec, new NoOpLockManager(), new Tracing());
  }

  private static Flow flowWithTransformer(Function<String, String> transformer) {
    Function<Message, String> emptyReader = m -> "";
    Function<String, Message> emptyWriter = s -> new Message();
    var spec =
        FlowBuilder.flow(Message.class, String.class, String.class)
            .reader(emptyReader)
            .transformer(transformer)
            .writerSendingMessage(emptyWriter)
            .build();
    return new Flow(spec, new NoOpLockManager(), new Tracing());
  }

  public static Flow flowBlockingAllThreads() {
    return flowWithTransformer(new ThreadBlockingTransformer<>());
  }

  public static Flow messageProcessor(Function<TestMessage, Message> function) {
    var spec = FlowBuilder.messageProcessor(TestMessage.class).process(function).build();
    return new Flow(spec, new NoOpLockManager(), new Tracing());
  }
}
