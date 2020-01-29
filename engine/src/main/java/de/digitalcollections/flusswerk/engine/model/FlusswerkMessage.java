package de.digitalcollections.flusswerk.engine.model;

/**
 * Base class for implementations of Message.
 *
 * @param <T> The data type used for the identifier.
 */
public abstract class FlusswerkMessage<T> implements Message<T> {

  private Envelope envelope;

  protected T id;

  public FlusswerkMessage() {
    this.envelope = new Envelope();
    this.id = null;
  }

  public FlusswerkMessage(T id) {
    this.envelope = new Envelope();
    this.id = id;
  }

  @Override
  public Envelope getEnvelope() {
    return envelope;
  }

  @Override
  public T getId() {
    return id;
  }
}
