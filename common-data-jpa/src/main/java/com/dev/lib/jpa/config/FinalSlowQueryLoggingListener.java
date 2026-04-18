package com.dev.lib.jpa.config;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public class FinalSlowQueryLoggingListener implements QueryExecutionListener {

    private final long thresholdMillis;
    private final Logger logger;
    private final DefaultQueryLogEntryCreator entryCreator;

    public FinalSlowQueryLoggingListener(Duration threshold, String loggerName) {

        this.thresholdMillis = threshold.toMillis();
        this.logger = LoggerFactory.getLogger(loggerName);
        this.entryCreator = new DefaultQueryLogEntryCreator();
        this.entryCreator.setMultiline(true);
    }

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {

        if (execInfo.getElapsedTime() < thresholdMillis) {
            return;
        }
        logger.warn("SlowQuery {}", entryCreator.getLogEntry(execInfo, queryInfoList, true, true, false));
    }
}
