package de.digitalcollections.flusswerk.engine.exceptions;

/**
 * @deprecated Use {@link de.digitalcollections.flusswerk.engine.exceptions.StopProcessingException}
 * instead.
 *
 */
@Deprecated
public class FinallyFailedProcessException extends StopProcessingException {

  public FinallyFailedProcessException(Throwable cause) {
    super("Using FinallyFailedProcessException is deprecated", cause);
  }

  public FinallyFailedProcessException(String message) {
    super(message);
  }

  public FinallyFailedProcessException(String message, Throwable cause) {
    super(message, cause);
  }
}
