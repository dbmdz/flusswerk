package de.digitalcollections.flusswerk.engine.exceptions;

public class RetriableProcessException extends RuntimeException {

  public RetriableProcessException(Throwable cause) {
    super(cause);
  }

  public RetriableProcessException(String message) {
    super(message);
  }

}
