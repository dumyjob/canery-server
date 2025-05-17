package com.github.shen.canary.server.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "deploy_tasks")
@Data
public class Deployment {
    @Id
    private String id;           // 任务ID（Celery Task ID）
    private Long projectId;
    private String branch;
    private String env;
    private String status;      // PENDING, DEPLOYING, SUCCESS, FAILED
    private String externalId;  // 阿里云资源栈ID
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}