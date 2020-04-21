package com.github.dbmdz.flusswerk.framework.exceptions;

/** This exception is thrown when the workflow cannot be started because of configuration issues. */
public class WorkflowSetupException extends RuntimeException {

  public WorkflowSetupException(Throwable cause) {
    super(cause);
  }

  public WorkflowSetupException(String message) {
    super(message);
  }
}
