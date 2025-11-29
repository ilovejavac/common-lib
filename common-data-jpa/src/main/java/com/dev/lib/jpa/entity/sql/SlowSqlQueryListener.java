package com.dev.lib.jpa.entity.sql;

import com.dev.lib.jpa.config.AppSqlMonitorProperties;
import com.dev.lib.security.util.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component  // 只要这个
@RequiredArgsConstructor
public class SlowSqlQueryListener implements QueryExecutionListener {

    private final AppSqlMonitorProperties properties;
    private final SlowSqlLogRepository slowSqlLogRepository;

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        // 不需要处理
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        if (Boolean.FALSE.equals(properties.getEnabled())) {
            return;
        }

        long duration = execInfo.getElapsedTime();

        if (duration > properties.getSlowThreshold()) {
            String sql = queryInfoList.stream()
                    .map(QueryInfo::getQuery)
                    .collect(Collectors.joining("; "));

            if (Boolean.TRUE.equals(properties.getLogEnabled())) {
                log.warn("Slow SQL: {}ms - {}", duration, sql);
            }

            if (Boolean.TRUE.equals(properties.getSaveEnabled())) {
                saveSlowSql(sql, duration);
            }
        }
    }

    private void saveSlowSql(String sql, long executeTime) {
        try {
            SlowSqlLog logEntity = new SlowSqlLog();
            logEntity.setSql(sql);
            logEntity.setExecuteTime(executeTime);
            logEntity.setMethod(getCallerMethod());
            logEntity.setStackTrace(getStackTrace());
            logEntity.setUsername(SecurityContextHolder.getUsername());

            slowSqlLogRepository.save(logEntity);
        } catch (Exception e) {
            log.error("Failed to save slow SQL log", e);
        }
    }

    private String getCallerMethod() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : elements) {
            String className = element.getClassName();
            if (!className.startsWith("java.") &&
                    !className.startsWith("org.") &&
                    !className.startsWith("net.ttddyy") &&
                    !className.contains("SlowSql")) {
                return element.toString();
            }
        }
        return "unknown";
    }

    private String getStackTrace() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (StackTraceElement element : elements) {
            String className = element.getClassName();
            if (!className.startsWith("java.lang.Thread") &&
                    !className.startsWith("net.ttddyy") &&
                    !className.contains("SlowSql")) {
                sb.append(element).append("\n");
                if (++count >= 10) break;
            }
        }
        return sb.toString();
    }
}