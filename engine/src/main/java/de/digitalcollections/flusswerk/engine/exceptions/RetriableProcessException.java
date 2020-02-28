package de.digitalcollections.flusswerk.engine.exceptions;

/**
 * @deprecated Use {@link de.digitalcollections.flusswerk.engine.exceptions.RetryProcessingException}
 *
 */
@Deprecated
public class RetriableProcessException extends RuntimeException {

  public RetriableProcessException(Throwable cause) {
    super(cause);
  }

  public RetriableProcessException(String message) {
    super(message);
  }
}
