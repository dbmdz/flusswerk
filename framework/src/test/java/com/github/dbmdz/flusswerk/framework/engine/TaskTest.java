package com.github.dbmdz.flusswerk.framework.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("A Task")
class TaskTest {

  private static Stream<Arguments> tasks() {
    return Stream.of(
        arguments(new Task(new Message("tracing"), 0), new Task(new Message("tracing"), 0), true),
        arguments(new Task(new Message("X_X_X_X"), 0), new Task(new Message("tracing"), 0), false),
        arguments(new Task(new Message("tracing"), 9), new Task(new Message("tracing"), 0), false));
  }

  @DisplayName("should be ordered by priority descending (higher priority → lower position)")
  @Test
  void compareTo() {
    Task highPriority = new Task(new Message("high"), 10);
    Task lowPriority = new Task(new Message("low"), 1);
    assertThat(highPriority).isLessThan(lowPriority);
  }

  @DisplayName("should be returned from priority queue in descending order")
  @Test
  void shouldReturnFromPriorityQueueInDescendingPriority() {
    Task highPriority = new Task(new Message("high"), 10);
    Task midPriority = new Task(new Message("mid"), 5);
    Task lowPriority = new Task(new Message("low"), 1);

    PriorityBlockingQueue<Task> tasks = new PriorityBlockingQueue<>();
    tasks.add(highPriority);
    tasks.add(lowPriority);
    tasks.add(midPriority);

    List<Task> actual = new ArrayList<>();
    tasks.drainTo(actual);
    assertThat(actual).containsExactly(highPriority, midPriority, lowPriority);
  }

  @DisplayName("should be equal to equal tasks")
  @ParameterizedTest
  @MethodSource("tasks")
  void shouldBeEqual(Task a, Task b, boolean equal) {
    assertThat(a.equals(b)).isEqualTo(equal);
  }

  @DisplayName("should have same hashCode as equal tasks")
  @ParameterizedTest
  @MethodSource("tasks")
  void shouldHaveSameHashCode(Task a, Task b, boolean equal) {
    assertThat(a.hashCode() == b.hashCode()).isEqualTo(equal);
  }
}
