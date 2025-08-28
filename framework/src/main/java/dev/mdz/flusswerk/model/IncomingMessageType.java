package dev.mdz.flusswerk.model;

import static java.util.Objects.requireNonNull;

/** Register a custom message implementation via Spring. */
public class IncomingMessageType {

  private final Class<? extends Message> messageClass;

  private Class<?> mixin;

  /** Use Message for incoming messages. */
  public IncomingMessageType() {
    this(Message.class);
  }

  /**
   * Provide a custom {@link Message} implementation with default serialization and deserialization
   * settings.
   *
   * @param cls the custom {@link Message} implementation
   */
  public IncomingMessageType(Class<? extends Message> cls) {
    this.messageClass = requireNonNull(cls);
  }

  /**
   * Provide a custom {@link Message} implementation with custom serialization and deserialization
   * settings.
   *
   * @param cls custom {@link Message} implementation
   * @param mixin custom Jackson mixin for specific serialization and deserialization settings
   */
  public IncomingMessageType(Class<? extends Message> cls, Class<?> mixin) {
    this.messageClass = requireNonNull(cls);
    this.mixin = requireNonNull(mixin);
  }

  /**
   * @return the class of the custom message implementation
   */
  public Class<? extends Message> getMessageClass() {
    return messageClass;
  }

  /**
   * @return the custom Jackson mixin for the {@link Message} implementation
   */
  public Class<?> getMixin() {
    return mixin;
  }

  /**
   * @return true, if there is also a Jackson mixin for the message class
   */
  public boolean hasMixin() {
    return this.mixin != null;
  }
}
