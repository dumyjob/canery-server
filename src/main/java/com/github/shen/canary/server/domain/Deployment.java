package com.github.shen.canary.server.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

//@Entity
//@Table(name = "deployments")
@Data
public class Deployment {
    @Id
    private String id;           // 任务ID（Celery Task ID）
    private String projectId;
    private String status;      // PENDING, DEPLOYING, SUCCESS, FAILED
    private String externalId;  // 阿里云资源栈ID
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}