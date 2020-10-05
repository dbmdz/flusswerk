package com.github.dbmdz.flusswerk.framework.reporting;

import de.huxhorn.sulky.ulid.ULID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class Tracing {

  private final ConcurrentHashMap<Long, List<String>> tracingPath;
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
    this.tracingPath = new ConcurrentHashMap<>();
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
    this.tracingPath.put(currentThread.id(), tracingPath);
  }

  public void deregister() {
    tracingPath.remove(currentThread.id());
  }

  public List<String> tracingPath() {
    return tracingPath.getOrDefault(currentThread.id(), Collections.emptyList());
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
}
