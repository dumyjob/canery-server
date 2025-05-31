package com.github.shen.canary.server.entity;

import com.github.shen.canary.server.web.request.CommitEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "git_repo")
    private String gitRepo;
    private String branch;

    @Column(name = "commit_id")
    private String commitId;
    @Column(name = "commit_message")
    private String commitMessage;

    private String env;
    private String status;      // PENDING, DEPLOYING, SUCCESS, FAILED

    @Column(name = "external_id")
    private String externalId;  // 阿里云资源栈ID

    @Column(name = "start_time")
    private LocalDateTime startTime;
    @Column(name = "end_time")
    private LocalDateTime endTime;

    public Deployment deployed(final CommitEntry commit) {
        this.commitId = commit.getCommitId();
        this.commitMessage = commit.getMessage();
        return this;
    }
}