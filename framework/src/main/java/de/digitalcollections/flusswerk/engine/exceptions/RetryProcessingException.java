package de.digitalcollections.flusswerk.engine.exceptions;

import java.util.Optional;
import java.util.function.Supplier;

/** Throw this exception in case of errors that might go away when trying again later. */
public class RetryProcessingException extends RuntimeException {

  /** @param message The message, possibly including format strings */
  public RetryProcessingException(String message) {
    super(message); // String-only constructor is needed for supplier
  }

  /**
   * @param message The message, possibly including format strings
   * @param args Arguments for the format string in <code>message</code>
   */
  public RetryProcessingException(String message, Object... args) {
    super(String.format(message, args));
  }

  private RetryProcessingException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Add the exceptions cause to the exception without interfering with the format string arguments.
   *
   * @param cause The cause to add
   * @return An augmented version of the exception including the cause
   */
  public RetryProcessingException causedBy(Throwable cause) {
    return new RetryProcessingException(this.getMessage(), cause);
  }

  /**
   * Fluid interface to create exception suppliers, e.g. for use in {@link
   * Optional#orElseThrow(Supplier)}.
   *
   * @param message The message, possibly including format strings
   * @param args Arguments for the format string in <code>message</code>
   * @return A supplier for this exception than can be further customized
   */
  public static ExceptionSupplier<RetryProcessingException> because(
      String message, Object... args) {
    return new ExceptionSupplier<>(RetryProcessingException.class, message, args);
  }
}
