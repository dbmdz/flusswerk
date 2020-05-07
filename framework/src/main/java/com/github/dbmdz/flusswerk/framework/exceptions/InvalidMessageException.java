package com.github.dbmdz.flusswerk.framework.exceptions;

import com.github.dbmdz.flusswerk.framework.model.Message;

public class InvalidMessageException extends Exception {

  private final Message message;

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
