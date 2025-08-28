package dev.mdz.flusswerk.reporting;

import de.huxhorn.sulky.ulid.ULID;
import dev.mdz.flusswerk.model.Message;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Tracing {

  private final ConcurrentHashMap<Long, List<String>> tracingPathForThread;
  private final CurrentThread currentThread;
  private final ULID ulid;

  static class CurrentThread {

    public long id() {
      return Thread.currentThread().getId();
    }
  }

  public Tracing() {
    this(new CurrentThread());
  }

  /**
   * Constructor for testing.
   *
   * @param currentThread Proxy to get the current threads id (so mocking can be used for testing)
   */
  Tracing(CurrentThread currentThread) {
    this.tracingPathForThread = new ConcurrentHashMap<>();
    this.currentThread = currentThread;
    this.ulid = new ULID();
  }

  /**
   * Saves the tracing path and adds a new tracing ID for the current workflow job.
   *
   * @param ids The ids from the incoming message
   */
  public void register(List<String> ids) {
    List<String> tracingPath;
    if (ids == null || ids.isEmpty()) {
      tracingPath = List.of(ulid.nextULID());
    } else {
      List<String> path = new ArrayList<>(ids);
      path.add(ulid.nextULID());
      tracingPath = List.copyOf(path); // make immutable
    }
    tracingPathForThread.put(currentThread.id(), tracingPath);
  }

  /** Delete tracing information for current Thread. */
  public void deregister() {
    tracingPathForThread.remove(currentThread.id());
  }

  /**
   * @return The tracing information for the current thread.
   */
  public List<String> tracingPath() {
    return tracingPathForThread.getOrDefault(currentThread.id(), Collections.emptyList());
  }

  /**
   * Generates a new tracing path that contains exactly one tracing id. This is used whenever the
   * thread-map based tracing cannot be used (e.g. in an application that does not work with
   * incoming messages).
   *
   * @return a new tracing path
   */
  public List<String> newPath() {
    return List.of(ulid.nextULID());
  }

  /**
   * Sets the correct tracing path for the given message if not already manually set.
   *
   * @param message The message to set the tracing path for.
   */
  public void ensureFor(Message message) {
    if (message.getTracing() == null || message.getTracing().isEmpty()) {
      message.setTracing(tracingPath());
    }
  }

  /**
   * Sets the correct tracing path for the given messages.
   *
   * @param messages The messages to set the tracing path for.
   */
  public void ensureFor(Collection<? extends Message> messages) {
    messages.forEach(this::ensureFor);
  }
}
