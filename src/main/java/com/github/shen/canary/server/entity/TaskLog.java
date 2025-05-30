package com.github.shen.canary.server.entity;

import com.github.shen.canary.server.web.request.LogEntry;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.Id;


import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "deploy_logs", indexes = {
        @Index(name = "idx_task_step", columnList = "taskId,step"),
        @Index(name = "idx_log_time", columnList = "logTime DESC")
})
public class TaskLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String taskId; // 对应构建#124

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LogStep step; // 对应进度条阶段

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LogLevel level;

    @Lob
    @Column(nullable = false)
    private String content; // 日志内容

    @Column(nullable = false)
    private LocalDateTime logTime; // 时间格式：2025-05-14 17:18:25

    @Column(length = 50)
    private String errorCode; // 错误类型（如SYNTAX_ERROR）

    @Lob
    private String suggestion; // 修复建议（如"检查第42行语法"）

    public TaskLog(String taskId, LogEntry log) {
        this.taskId = taskId;
        this.step = log.getStep() != null ? LogStep.valueOf(log.getStep()) : LogStep.CHECKOUT;
        this.level = log.getLevel() != null ? LogLevel.valueOf(log.getLevel()) : LogLevel.INFO;
        this.content = log.getContent();
    }

    // 枚举定义
    public enum LogStep {
        CHECKOUT, BUILD, TEST, DEPLOY
    }

    public enum LogLevel {
        INFO, WARN, ERROR
    }

    // Getters & Setters
}
