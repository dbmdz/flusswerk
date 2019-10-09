package de.digitalcollections.flusswerk.engine;

public class EngineStats {

  private final int concurrentWorkers;

  private final int activeWorkers;

  private final int availableWorkers;

  public EngineStats(int concurrentWorkers, int activeWorkers, int availableWorkers) {
    this.concurrentWorkers = concurrentWorkers;
    this.activeWorkers = activeWorkers;
    this.availableWorkers = availableWorkers;
  }

  /** @return the maximum number of concurrent workers */
  public int getConcurrentWorkers() {
    return concurrentWorkers;
  }

  /** @return the number of workers currently processing a message */
  public int getActiveWorkers() {
    return activeWorkers;
  }

  /** @return the number of idle workers */
  public int getAvailableWorkers() {
    return availableWorkers;
  }
}
