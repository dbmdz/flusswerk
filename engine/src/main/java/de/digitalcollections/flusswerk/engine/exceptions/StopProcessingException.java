package de.digitalcollections.flusswerk.engine.exceptions;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Throw this exception in case of errors that won't go away when trying again later.
 */
public class StopProcessingException extends RuntimeException {

  /**
   *
   * @param message The message, possibly including format strings
   * @param args Arguments for the format string in <code>message</code>
   */
  public StopProcessingException(String message, Object... args) {
    super(String.format(message, args));
  }

  private StopProcessingException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Add the exceptions cause to the exception without interfering with the format string arguments.
   * @param cause The cause to add
   * @return An augmented version of the exception including the cause
   */
  public StopProcessingException causedBy(Throwable cause) {
    return new StopProcessingException(this.getMessage(), cause);
  }

  /**
   * Fluid interface to create exception suppliers, e.g. for use in {@link Optional#orElseThrow(Supplier)}.
   * @param message The message, possibly including format strings
   * @param args Arguments for the format string in <code>message</code>
   * @return A supplier for this exception than can be further customized
   */
  public static StopProcessingExceptionSupplier because(String message, Object... args) {
    return new StopProcessingExceptionSupplier(message, args);
  }

  /**
   * A supplier for RetryProcessingException, e.g. for use in {@link Optional#orElseThrow(Supplier)}.
   */
  public static class StopProcessingExceptionSupplier
      implements Supplier<StopProcessingException> {

    private String message;

    private Object[] args;

    private Throwable cause;

    public StopProcessingExceptionSupplier(String message, Object[] args) {
      this.message = message;
      this.args = args;
    }

    @Override
    public StopProcessingException get() {
      return new StopProcessingException(String.format(message, args), cause);
    }

    /**
     * Add the exceptions cause to the supplier without interfering with the format string arguments.
     * @param cause The cause to add
     * @return An augmented version of the supplier including the cause
     */
    public StopProcessingExceptionSupplier causedBy(Throwable cause) {
      this.cause = cause;
      return this;
    }
  }
}
