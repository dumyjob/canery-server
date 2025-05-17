package com.github.shen.canary.server.repository;

import com.github.shen.canary.server.entity.TaskLog;

public interface LogRepository {
    void save(TaskLog taskLog);
}
