package dev.mdz.flusswerk.exceptions;

import java.util.function.Supplier;

/**
 * Generic supplier for exceptions.
 *
 * @param <T> The type of the exception to supply
 */
public class ExceptionSupplier<T extends Exception> implements Supplier<T> {

  private final Class<T> exceptionClass;

  private final String message;

  private final Object[] args;

  private Throwable cause;

  public ExceptionSupplier(Class<T> exceptionClass, String message, Object[] args) {
    this.exceptionClass = exceptionClass;
    this.message = message;
    this.args = args;
    this.cause = null;
  }

  @Override
  public T get() {
    try {
      return createException();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Cannot create instance of " + exceptionClass, e);
    }
  }

  private T createException() throws ReflectiveOperationException {
    String renderedMessage = String.format(message, args);
    if (cause == null) {
      var constructor = exceptionClass.getDeclaredConstructor(String.class);
      return constructor.newInstance(renderedMessage);
    } else {
      var constructor = exceptionClass.getDeclaredConstructor(String.class, Throwable.class);
      constructor.setAccessible(true);
      return constructor.newInstance(renderedMessage, cause);
    }
  }

  /**
   * Add the exceptions cause to the supplier without interfering with the format string arguments.
   *
   * @param cause The cause to add
   * @return An augmented version of the supplier including the cause
   */
  public ExceptionSupplier<T> causedBy(Throwable cause) {
    this.cause = cause;
    return this;
  }
}
