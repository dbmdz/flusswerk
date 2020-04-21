package com.github.dbmdz.flusswerk.spring.boot.starter;

import com.github.dbmdz.flusswerk.framework.model.Message;

/**
 * Mapping declaration for <code>Message</code> implementations and Jackson mixins to allow
 * registration as a Spring bean.
 *
 * @param <T> The message class
 * @deprecated Use the easier and more flexible {@link MessageImplementation} instead
 */
@Deprecated
public class MessageMapping<T extends Message<?>> {

  private Class<T> messageClass;

  private Class<?> mixin;

  /**
   * Creates an immutable mapping instance.
   *
   * @param messageClass The message class to register a mixin for
   * @param mixin The Jackson mixin to register for the <code>Message</code> class
   */
  public MessageMapping(Class<T> messageClass, Class<?> mixin) {
    this.messageClass = messageClass;
    this.mixin = mixin;
  }

  /** @return The message class to register a mixin for. */
  public Class<T> getMessageClass() {
    return messageClass;
  }

  /** @return The mixin to register for the message class. */
  public Class<?> getMixin() {
    return mixin;
  }
}
