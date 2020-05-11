package com.github.dbmdz.flusswerk.spring.boot.starter;

import static java.util.Objects.requireNonNull;

import com.github.dbmdz.flusswerk.framework.model.Message;

/** Register a custom message implementation via Spring. */
public class MessageImplementation<M extends Message> {

  private final Class<M> messageClass;

  private Class<?> mixin;

  /**
   * Provide a custom {@link Message} implmentation with default serialization and deserialization
   * settings.
   *
   * @param cls the custom {@link Message} implementation
   */
  public MessageImplementation(Class<M> cls) {
    this.messageClass = requireNonNull(cls);
  }

  /**
   * Provide a custom {@link Message} implmentation with custom serialization and deserialization
   * settings.
   *
   * @param cls custom {@link Message} implementation
   * @param mixin custom Jackson mixin for specific serialization and deserialization settings
   */
  public MessageImplementation(Class<M> cls, Class<?> mixin) {
    this.messageClass = requireNonNull(cls);
    this.mixin = requireNonNull(mixin);
  }

  /** @return the class of the custom message implementation */
  public Class<M> getMessageClass() {
    return messageClass;
  }

  /** @return the custom Jackson mixin for the {@link Message} implementation */
  public Class<?> getMixin() {
    return mixin;
  }

  /** @return true, if there is also a Jackson mixin for the message class */
  public boolean hasMixin() {
    return this.mixin != null;
  }

  static MessageImplementation<Message> getDefault() {
    return new MessageImplementation<>(Message.class);
  }
}
