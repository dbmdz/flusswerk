package de.digitalcollections.flusswerk.engine.model;

/**
 * Base class for implementations of Message.
 *
 * @param <T> The data type used for the identifier.
 */
public abstract class FlusswerkMessage<T> implements Message<T> {

  private Envelope envelope;

  protected T identifier;

  public FlusswerkMessage() {
    this.envelope = new Envelope();
    this.identifier = null;
  }

  public FlusswerkMessage(T identifier) {
    this.envelope = new Envelope();
    this.identifier = identifier;
  }

  @Override
  public Envelope getEnvelope() {
    return envelope;
  }

  @Override
  public T getId() {
    return identifier;
  }
}
