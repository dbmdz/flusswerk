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

@DisplayName("The DefaultProcessReport")
class DefaultProcessReportTest {

  private DefaultProcessReport defaultProcessReport;
  private ListAppender<ILoggingEvent> listAppender;

  @BeforeEach
  void setUp() {
    Logger logger = (Logger) LoggerFactory.getLogger(DefaultProcessReport.class);
    logger.setLevel(Level.DEBUG);
    listAppender = new ListAppender<>();
    listAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    listAppender.start();
    logger.addAppender(listAppender);
    defaultProcessReport = new DefaultProcessReport("testapp");
  }

  @DisplayName("should report success")
  @Test
  void reportSuccess() {
    Message message = new Message();
    defaultProcessReport.reportSuccess(message);
    assertThat(listAppender.list).first()
        .hasFieldOrPropertyWithValue("level", Level.INFO)
        .hasFieldOrPropertyWithValue("formattedMessage", "testapp successful");
  }

  @DisplayName("should report failure")
  @Test
  void reportFail() {
    var message = new Message("123");
    var e = new StopProcessingException("stop now");
    defaultProcessReport.reportFail(message, e);
    assertThat(listAppender.list).first()
        .hasFieldOrPropertyWithValue("level", Level.ERROR)
        .hasFieldOrPropertyWithValue("formattedMessage", "testapp failed terminally (message=" + message + ", exception=" + e +")");
  }

  @DisplayName("should report failure after too many retries")
  @Test
  void reportFailAfterMaxRetries() {
    var message = new Message("123");
    var e = new StopProcessingException("stop now");
    defaultProcessReport.reportFailAfterMaxRetries(message, e);
    assertThat(listAppender.list).first()
        .hasFieldOrPropertyWithValue("level", Level.ERROR)
        .hasFieldOrPropertyWithValue("formattedMessage", "testapp failed after maximum number of retries (message=" + message + ", exception=" + e +")");
  }

  @DisplayName("should report failure with planned retries")
  @Test
  void reportReject() {
    var message = new Message("123");
    var e = new RetryProcessingException("stop now");
    defaultProcessReport.reportReject(message, e);
    assertThat(listAppender.list).first()
        .hasFieldOrPropertyWithValue("level", Level.WARN)
        .hasFieldOrPropertyWithValue("formattedMessage", "testapp rejected for retry (message=" + message + ", exception=" + e +")");

  }
}