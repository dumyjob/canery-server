package com.github.shen.canary.server.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

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
