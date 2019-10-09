package de.digitalcollections.flusswerk.engine.exceptions;

public class FinallyFailedProcessException extends RuntimeException {

  public FinallyFailedProcessException(Throwable cause) {
    super(cause);
  }

  public FinallyFailedProcessException(String message) {
    super(message);
  }

  public FinallyFailedProcessException(String message, Throwable cause) {
    super(message, cause);
  }
}
