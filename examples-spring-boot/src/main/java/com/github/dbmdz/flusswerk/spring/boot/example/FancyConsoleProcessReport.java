package com.github.dbmdz.flusswerk.spring.boot.example;

import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.model.Message;
import com.github.dbmdz.flusswerk.framework.reporting.ProcessReport;
import com.github.dbmdz.flusswerk.spring.boot.example.model.Greeting;

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
    System.out.print("\uD83D\uDE45 " + ((Greeting) message).getId());
    if (e != null) {
      System.out.println(":");
      System.out.println(e.toString());
    } else {
      System.out.println(".");
    }
  }
}
