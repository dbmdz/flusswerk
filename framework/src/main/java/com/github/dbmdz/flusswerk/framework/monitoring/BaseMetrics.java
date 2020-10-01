package com.github.dbmdz.flusswerk.framework.monitoring;

/**
 * Class remains until next major release for API compatibility in case an app used this for
 * implementing monitoring features.
 */
@Deprecated
public class BaseMetrics extends DefaultFlowMetrics {

  public BaseMetrics(MeterFactory meterFactory) {
    super(meterFactory);
  }
}
