package com.github.dbmdz.flusswerk.framework.monitoring;

import com.github.dbmdz.flusswerk.framework.flow.FlowInfo;
import java.util.function.Consumer;

/** Interface for all metrics collectors that need access to the execution information of a flow. */
public interface FlowMetrics extends Consumer<FlowInfo> {}
