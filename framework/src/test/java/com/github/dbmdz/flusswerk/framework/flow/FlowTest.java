package com.github.dbmdz.flusswerk.framework.flow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.dbmdz.flusswerk.framework.TestMessage;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.Tracing;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("The Flow")
@ExtendWith(MockitoExtension.class)
class FlowTest {

  @Mock private TestMessage incomingMessage;
  @Mock private Function<Message, Object> reader;
  @Mock private Function<Object, Object> transformer;
  @Mock private Function<Object, Collection<Message>> writer;
  @Mock private Tracing tracing;
  @Mock private Runnable cleanup;

  @BeforeEach
  void setUp() {
    when(reader.apply(any(Message.class))).thenReturn("test in");
    when(transformer.apply(anyString())).thenReturn("test out");
    when(writer.apply(anyString())).thenReturn(List.of(new Message()));
    FlowSpec flowSpec = new FlowSpec(reader, transformer, writer, cleanup, null);
    Flow flow = new Flow(flowSpec, tracing);

    // do the processing, because each test would do the same
    flow.process(incomingMessage);
  }

  @DisplayName("should call read")
  @Test
  void shouldCallRead() {
    verify(reader).apply(any(Message.class));
  }

  @DisplayName("should call transform")
  @Test
  void shouldCallTransform() {
    verify(transformer).apply("test in");
  }

  @DisplayName("should call write")
  @Test
  void shouldCallWrite() {
    verify(writer).apply("test out");
  }

  @DisplayName("should call cleanup")
  @Test
  void shouldCallCleanup() {
    verify(cleanup).run();
  }

  @DisplayName("should get id from incomingMessage")
  @Test
  void shouldGetIdFromIncomingMessage() {
    verify(incomingMessage).getId();
  }
}
