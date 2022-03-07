package com.github.dbmdz.flusswerk.framework.exceptions;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import com.github.dbmdz.flusswerk.framework.model.Message;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Indicates that processing should be skipped from the point on where this exception is thrown. */
public class SkipProcessingException extends RuntimeException {

  private final List<Message> outgoingMessages = new ArrayList<>();

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
}
