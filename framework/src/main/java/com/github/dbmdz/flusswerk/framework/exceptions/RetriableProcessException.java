package com.github.dbmdz.flusswerk.framework.exceptions;

/** @deprecated Use {@link RetryProcessingException} */
@Deprecated
public class RetriableProcessException extends RuntimeException {

  public RetriableProcessException(Throwable cause) {
    super(cause);
  }

  public RetriableProcessException(String message) {
    super(message);
  }
}
