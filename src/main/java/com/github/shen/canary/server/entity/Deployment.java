package com.github.shen.canary.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Entity
@Table(name = "deploy_tasks")
@Data
public class Deployment {
    @Id
    private String id;           // 任务ID（Celery Task ID）

    @Column(name = "project_id")
    private Long projectId;
    private String branch;
    private String env;
    private String status;      // PENDING, DEPLOYING, SUCCESS, FAILED

    @Column(name = "external_id")
    private String externalId;  // 阿里云资源栈ID

    @Column(name = "start_time")
    private LocalDateTime startTime;
    @Column(name = "end_time")
    private LocalDateTime endTime;
}