package com.github.shen.canary.server.repository.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.shen.canary.server.entity.TaskLog;
import com.github.shen.canary.server.repository.LogRepository;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class LogRepositoryImpl implements LogRepository {

    private final RedisTemplate<String, Object> redisTemplate;
//    private final LogRepository jpaRepo; // mysql
//    private final S3Template s3Template; //
private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // 关键配置：禁用 Unicode 转义
        objectMapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
    }


    @SneakyThrows
    @Override
    public void save(TaskLog taskLog) {
        // 实时任务处理中 -> Redis
        String streamKey = "logs:" + taskLog.getTaskId();
        String json = objectMapper.writeValueAsString(taskLog);
        log.info("redis-logs:{}", json);
        redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                    .ofObject(json)
                        .withStreamKey(streamKey)
        );

    }
}
