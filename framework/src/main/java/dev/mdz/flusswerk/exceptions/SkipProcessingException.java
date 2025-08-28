package dev.mdz.flusswerk.exceptions;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import dev.mdz.flusswerk.model.Message;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Indicates that processing should be skipped from the point on where this exception is thrown. */
public class SkipProcessingException extends RuntimeException {

  private final List<Message> outgoingMessages = new ArrayList<>();

  private SkipProcessingException(String message, Throwable cause, List<Message> outgoingMessages) {
    super(message, cause);
    this.outgoingMessages.addAll(outgoingMessages);
  }

  /**
   * Skip processing with an explanation of why.
   *
   * @param message Why should processing be skipped?
   */
  public SkipProcessingException(String message) {
    super(message);
  }

  /**
   * Add messages to send to the next workflow job.
   *
   * @param messages The messages to send.
   * @return <code>this</code> for a fluent interface.
   */
  public SkipProcessingException send(Message... messages) {
    return send(asList(messages));
  }

  /**
   * Add messages to send to the next workflow job.
   *
   * @param messages The messages to send.
   * @return <code>this</code> for a fluent interface.
   */
  public SkipProcessingException send(Collection<Message> messages) {
    outgoingMessages.addAll(messages);
    return this;
  }

  /**
   * @return The messages that should be sent to the next workflow job.
   */
  public List<Message> getOutgoingMessages() {
    return unmodifiableList(outgoingMessages);
  }

  /**
   * Add the exceptions cause to the exception without interfering with the format string arguments.
   *
   * @param cause The cause to add
   * @return An augmented version of the exception including the cause
   */
  public SkipProcessingException causedBy(Throwable cause) {
    return new SkipProcessingException(getMessage(), cause, outgoingMessages);
  }

  /**
   * Fluid interface to create exception suppliers, e.g. for use in {@link
   * Optional#orElseThrow(Supplier)}.
   *
   * @param message The message, possibly including format strings
   * @param args Arguments for the format string in <code>message</code>
   * @return A supplier for this exception than can be further customized
   */
  public static ExceptionSupplier<SkipProcessingException> because(String message, Object... args) {
    return new ExceptionSupplier<>(SkipProcessingException.class, message, args);
  }
}
