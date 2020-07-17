package com.github.dbmdz.flusswerk.framework.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.dbmdz.flusswerk.framework.exceptions.RetryProcessingException;
import com.github.dbmdz.flusswerk.framework.exceptions.StopProcessingException;
import com.github.dbmdz.flusswerk.framework.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayName("The SilentProcessReport")
class SilentProcessReportTest {

  private SilentProcessReport silentProcessReport;
  private ListAppender<ILoggingEvent> listAppender;

  @BeforeEach
  void setUp() {
    Logger logger = (Logger) LoggerFactory.getLogger(DefaultProcessReport.class);
    logger.setLevel(Level.DEBUG);
    listAppender = new ListAppender<>();
    listAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    listAppender.start();
    logger.addAppender(listAppender);
    silentProcessReport = new SilentProcessReport();
  }


  @DisplayName("should report nothing on success")
  @Test
  void reportSuccess() {
    Message message = new Message();
    silentProcessReport.reportSuccess(message);
    assertThat(listAppender.list).isEmpty();
  }

  @DisplayName("should report nothing on fail")
  @Test
  void reportFail() {
    silentProcessReport.reportFail(new Message("123"), new StopProcessingException("stop now"));
    assertThat(listAppender.list).isEmpty();
  }

  @DisplayName("should report nothing on fail after too many retries")
  @Test
  void reportFailAfterMaxRetries() {
    silentProcessReport.reportFailAfterMaxRetries(new Message("123"), new StopProcessingException("stop now"));
    assertThat(listAppender.list).isEmpty();
  }

  @DisplayName("should report nothing on fail with retries")
  @Test
  void reportReject() {
    silentProcessReport.reportReject(new Message("123"), new RetryProcessingException("stop now"));
    assertThat(listAppender.list).isEmpty();
  }
}