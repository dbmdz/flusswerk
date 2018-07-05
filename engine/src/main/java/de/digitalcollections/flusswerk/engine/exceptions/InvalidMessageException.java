package de.digitalcollections.flusswerk.engine.exceptions;

import de.digitalcollections.flusswerk.engine.model.Message;

public class InvalidMessageException extends Exception {

  private Message message;

  public InvalidMessageException(Message message, String msg) {
    super(msg);
    this.message = message;
  }

  public InvalidMessageException(Message message, String msg, Throwable cause) {
    super(msg, cause);
    this.message = message;
  }

  public Message getInvalidMessage() {
    return message;
  }
}
