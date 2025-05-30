package com.github.shen.canary.server.web;

import com.github.shen.canary.server.entity.TaskLog;
import com.github.shen.canary.server.repository.LogRepository;
import com.github.shen.canary.server.web.request.LogEntry;
import com.github.shen.canary.server.web.request.StatusEntry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @see com.github.shen.canary.server.configuration.WebSocketConfig
 */
// 日志处理器
@RestController
@RequestMapping("/api/tasks/{taskId}")
@AllArgsConstructor
@Slf4j
public class TaskController {

    private SimpMessagingTemplate messagingTemplate;
    private LogRepository logRepository;


    @PostMapping("log")
    public void receiveLog(@PathVariable String taskId, @RequestBody LogEntry logEntry) {
        log.info("taskId: {},log:{}", taskId, logEntry);
        // 存储到数据库
        logRepository.save(new TaskLog(taskId, logEntry));
        
        // 广播到WebSocket频道
        messagingTemplate.convertAndSend("/topic/logs/" + taskId, logEntry);
    }

    @PatchMapping("status")
    public void taskStatus(@PathVariable String taskId, @RequestBody StatusEntry statusEntry) {
        log.info("taskId: {},status-log:{}", taskId, statusEntry);
    }
}