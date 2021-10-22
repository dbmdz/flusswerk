package com.github.dbmdz.flusswerk.framework.engine;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.Objects;

public class Task implements Comparable<Task> {

  private static final Runnable DO_NOTHING = () -> {};

  private final Message message;
  private final int priority;
  private final Runnable callback;

  public Task(Message message, int priority, Runnable callback) {
    this.message = requireNonNull(message);
    this.priority = priority;
    this.callback = Objects.requireNonNullElse(callback, DO_NOTHING);
  }

  public Task(Message message, int priority) {
    this(message, priority, DO_NOTHING);
  }

  public Message getMessage() {
    return message;
  }

  public int getPriority() {
    return priority;
  }

  @Override
  public int compareTo(Task other) {
    return other.priority - this.priority;
  }

  public void done() {
    this.callback.run();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Task task = (Task) o;
    return priority == task.priority
        && Objects.equals(message, task.message)
        && Objects.equals(callback, task.callback);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, priority, callback);
  }

  @Override
  public String toString() {
    return "Task{"
        + "message="
        + message
        + ", priority="
        + priority
        + ", callback="
        + callback
        + '}';
  }
}
