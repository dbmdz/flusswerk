package de.digitalcollections.flusswerk.spring.boot.example;

import de.digitalcollections.flusswerk.engine.exceptions.StopProcessingException;
import de.digitalcollections.flusswerk.engine.model.Message;
import de.digitalcollections.flusswerk.engine.reporting.ProcessReport;

public class FancyConsoleProcessReport implements ProcessReport {

  @Override
  public void reportSuccess(Message message) {
    print("\uD83C\uDF89", message, null);
  }

  @Override
  public void reportFail(Message message, StopProcessingException e) {
    print("\uD83D\uDCA5", message, e);
  }

  @Override
  public void reportFailAfterMaxRetries(Message message, Exception e) {
    print("❌", message, e);
  }

  @Override
  public void reportReject(Message message, Exception e) {
    print("\uD83D\uDE45", message, e);
  }

  private void print(String symbol, Message message, Exception e) {
    String id = "";
    if (message != null && message.getId() != null) {
      id = message.getId().toString();
    }
    System.out.print("\uD83D\uDE45 " + id);
    if (e != null) {
      System.out.println(":");
      System.out.println(e.toString());
    } else {
      System.out.println(".");
    }
  }
}
