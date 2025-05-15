package com.github.shen.canary.server.web;

import com.github.shen.canary.server.domain.LogEntry;
import com.github.shen.canary.server.domain.TaskLog;
import com.github.shen.canary.server.repository.LogRepository;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * @see com.github.shen.canary.server.configuration.WebSocketConfig
 */
// 日志处理器
@RestController
@RequestMapping("/api/tasks/{taskId}/log")
@AllArgsConstructor
public class LogController {

    private SimpMessagingTemplate messagingTemplate;
    private LogRepository logRepository;


    @PostMapping
    public void receiveLog(@PathVariable String taskId, @RequestBody LogEntry log) {
        // 存储到数据库
        logRepository.save(new TaskLog(taskId, log));
        
        // 广播到WebSocket频道
        messagingTemplate.convertAndSend("/topic/logs/" + taskId, log);
    }
}