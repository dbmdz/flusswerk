package com.github.dbmdz.flusswerk.framework.exceptions;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Throw this exception in case of errors that might go away when trying again later. */
public class RetryProcessingException extends RuntimeException {

  private final List<Message> messagesToRetry = new ArrayList<>();

  private final List<Message> messagesToSend = new ArrayList<>();

  /**
   * @param message The message, possibly including format strings
   */
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
    var exception = new RetryProcessingException(this.getMessage(), cause);
    exception.messagesToRetry.addAll(this.messagesToRetry);
    exception.messagesToSend.addAll(this.messagesToSend);
    return exception;
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

  public RetryProcessingException send(Message... messages) {
    messagesToSend.addAll(List.of(messages));
    return this;
  }

  public RetryProcessingException send(Iterable<Message> messages) {
    messages.forEach(messagesToSend::add);
    return this;
  }

  public RetryProcessingException retry(Message... messages) {
    messagesToRetry.addAll(List.of(messages));
    return this;
  }

  public RetryProcessingException retry(Iterable<Message> messages) {
    messages.forEach(messagesToRetry::add);
    return this;
  }

  public List<Message> getMessagesToRetry() {
    return messagesToRetry;
  }

  public List<Message> getMessagesToSend() {
    return messagesToSend;
  }

  public boolean hasMessagesToRetry() {
    return !messagesToRetry.isEmpty();
  }

  public boolean hasMessagesToSend() {
    return !messagesToSend.isEmpty();
  }

  /**
   * @return true, if there are messages to retry that are different from the message that caused
   *     the exception or if there are messages to send while still retrying other messages.
   */
  public boolean isComplex() {
    return hasMessagesToRetry() || hasMessagesToSend();
  }

  @Override
  public String toString() {
    String message = getLocalizedMessage();
    if (message == null) {
      return "RetryProcessingException";
    } else {
      return "RetryProcessingException: " + message;
    }
  }
}
