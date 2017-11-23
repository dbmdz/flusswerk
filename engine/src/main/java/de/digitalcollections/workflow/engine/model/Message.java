package de.digitalcollections.workflow.engine.model;

/**
 * A generic message as it will be sent over RabbitMQ.
 *
 * @param <ID> The type of the identifier field.
 */
public interface Message<ID> {

  /**
   * Technical metadata like timestamps and retries.
   *
   * @return An object containing the messages technical metadata.
   */
  Meta getMeta();

  /**
   * The optional message type is represented by an arbitrary string value to distinguish different events on the same queue.
   *
   * @return The message type.
   */
  String getType();

  /**
   * The optional ID can hold the identifier of an corresponding object (e.g. a book to be indexed).
   *
   * @return The corresponding object's id.
   */
  ID getId();

}
