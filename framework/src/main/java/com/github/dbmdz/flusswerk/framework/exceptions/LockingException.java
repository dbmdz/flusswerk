package com.github.dbmdz.flusswerk.framework.exceptions;

public class LockingException extends RuntimeException {

  public LockingException(String message) {
    super(message);
  }

  public LockingException(String message, Throwable cause) {
    super(message, cause);
  }
}
