package com.github.shen.canary.server.web;

import com.github.shen.canary.server.entity.LogEntry;
import com.github.shen.canary.server.entity.TaskLog;
import com.github.shen.canary.server.repository.LogRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * @see com.github.shen.canary.server.configuration.WebSocketConfig
 */
// 日志处理器
@RestController
@RequestMapping("/api/tasks/{taskId}/log")
@AllArgsConstructor
@Slf4j
public class TaskLogController {

    private SimpMessagingTemplate messagingTemplate;
    private LogRepository logRepository;


    @PostMapping
    public void receiveLog(@PathVariable String taskId, @RequestBody LogEntry logEntry) {
        log.info("taskId: {},log:{}", taskId, logEntry);
        // 存储到数据库
        logRepository.save(new TaskLog(taskId, logEntry));
        
        // 广播到WebSocket频道
        messagingTemplate.convertAndSend("/topic/logs/" + taskId, logEntry);
    }
}