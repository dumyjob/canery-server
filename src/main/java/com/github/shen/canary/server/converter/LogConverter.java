package com.github.shen.canary.server.converter;

import com.github.shen.canary.server.entity.TaskLog;
import com.github.shen.canary.server.utils.CanaryTimer;
import com.github.shen.canary.server.web.request.LogEntry;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LogConverter {


    public static LogEntry convert(final TaskLog taskLog) {
        LogEntry logEntry = new LogEntry();
        logEntry.setTimestamp(CanaryTimer.formate(taskLog.getLogTime()));
        logEntry.setContent(taskLog.getContent());
        logEntry.setLevel(String.valueOf(taskLog.getLevel()));
        logEntry.setStep(String.valueOf(taskLog.getStep()));
        logEntry.setHighlight(Boolean.TRUE);
        return logEntry;
    }


    public static List<LogEntry> convert(final List<TaskLog> taskLogs) {
        if (CollectionUtils.isEmpty(taskLogs)) {
            return Collections.emptyList();
        }
        return taskLogs.stream()
            .map(LogConverter::convert)
            .collect(Collectors.toList());
    }
}
