package dev.mdz.flusswerk.exceptions;

import dev.mdz.flusswerk.model.Envelope;

public class InvalidMessageException extends Exception {

  private final Envelope envelope;

  public InvalidMessageException(Envelope envelope, String msg) {
    super(msg);
    this.envelope = envelope;
  }

  public InvalidMessageException(Envelope envelope, String msg, Throwable cause) {
    super(msg, cause);
    this.envelope = envelope;
  }

  public Envelope getEnvelope() {
    return envelope;
  }
}
