package com.github.shen.canary.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Entity
@Table(name = "release_projects")
public class ReleaseProjectBean {

    @Column(name = "release_id")
    private Long releaseId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "deploy_branch")
    private String deployBranch;

}
