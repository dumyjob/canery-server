package com.github.shen.canary.server.repository;

import com.github.shen.canary.server.domain.TaskLog;

public interface LogRepository {
    void save(TaskLog taskLog);
}
