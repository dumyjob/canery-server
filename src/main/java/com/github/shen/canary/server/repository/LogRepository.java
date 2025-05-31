package com.github.shen.canary.server.repository;

import com.github.shen.canary.server.entity.TaskLog;

import java.util.List;

public interface LogRepository {
    void save(TaskLog taskLog);

    List<TaskLog> get(String taskId);
}
