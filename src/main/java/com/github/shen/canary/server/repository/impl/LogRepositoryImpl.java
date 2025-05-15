package com.github.shen.canary.server.repository.impl;

import com.github.shen.canary.server.domain.TaskLog;
import com.github.shen.canary.server.repository.LogRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class LogRepositoryImpl implements LogRepository {

    private final RedisTemplate<String, Object> redisTemplate;
//    private final LogRepository jpaRepo; // mysql
//    private final S3Template s3Template; //


    @Override
    public void save(TaskLog taskLog) {
        // 实时任务处理中 -> Redis
        String streamKey = "logs:" + taskLog.getTaskId();
        redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                        .ofObject(taskLog)
                        .withStreamKey(streamKey)
        );

    }
}
