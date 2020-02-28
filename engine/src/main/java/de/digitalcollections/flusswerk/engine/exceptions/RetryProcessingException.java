package de.digitalcollections.flusswerk.engine.exceptions;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Throw this exception in case of errors that might go away when trying again later.
 */
public class RetryProcessingException extends RuntimeException {

  /**
   *
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
   * @param cause The cause to add
   * @return An augmented version of the exception including the cause
   */
  public RetryProcessingException causedBy(Throwable cause) {
    return new RetryProcessingException(this.getMessage(), cause);
  }

  /**
   * Fluid interface to create exception suppliers, e.g. for use in {@link Optional#orElseThrow(Supplier)}.
   * @param message The message, possibly including format strings
   * @param args Arguments for the format string in <code>message</code>
   * @return A supplier for this exception than can be further customized
   */
  public static RetryProcessingExceptionSupplier because(String message, Object... args) {
    return new RetryProcessingExceptionSupplier(message, args);
  }

  /**
   * A supplier for RetryProcessingException, e.g. for use in {@link Optional#orElseThrow(Supplier)}.
   */
  public static class RetryProcessingExceptionSupplier
      implements Supplier<RetryProcessingException> {

    private String message;

    private Object[] args;

    private Throwable cause;

    public RetryProcessingExceptionSupplier(String message, Object[] args) {
      this.message = message;
      this.args = args;
    }

    @Override
    public RetryProcessingException get() {
      return new RetryProcessingException(String.format(message, args), cause);
    }

    /**
     * Add the exceptions cause to the supplier without interfering with the format string arguments.
     * @param cause The cause to add
     * @return An augmented version of the supplier including the cause
     */
    public RetryProcessingExceptionSupplier causedBy(Throwable cause) {
      this.cause = cause;
      return this;
    }
  }
}
