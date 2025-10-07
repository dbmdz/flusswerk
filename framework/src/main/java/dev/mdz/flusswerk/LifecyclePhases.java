package dev.mdz.flusswerk;

/** Define application life cycle phases for ordering startup and shutdown of components. */
public class LifecyclePhases {
  /** Setup/tear down connections to infrastructure components like RabbitMQ. */
  public static final int INFRASTRUCTURE = 0;

  /** Start/stop processing messages. */
  public static final int PROCESSING = 1;
}
