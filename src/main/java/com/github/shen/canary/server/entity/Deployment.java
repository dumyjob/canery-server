package com.github.shen.canary.server.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "deploy_tasks")
@Data
public class Deployment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "JDBC")
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