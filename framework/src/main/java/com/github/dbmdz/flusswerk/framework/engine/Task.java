package com.github.dbmdz.flusswerk.framework.engine;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.Objects;

public class Task implements Comparable<Task> {

  private final Message message;
  private final int priority;

  public Task(Message message, int priority) {
    this.message = requireNonNull(message);
    this.priority = priority;
  }

  public Message getMessage() {
    return message;
  }

  @Override
  public int compareTo(Task other) {
    return other.priority - this.priority;
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
    return priority == task.priority && Objects.equals(message, task.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, priority);
  }

  @Override
  public String toString() {
    return "Task{" + "message=" + message + ", priority=" + priority + '}';
  }
}
