package com.github.shen.canary.server.domain;

import com.github.shen.canary.server.entity.ReleaseProjectBean;
import lombok.Getter;

@Getter
public class ReleaseProject {

    private final Long releaseId;
    private final Long projectId;
    private final String branch;

    private ReleaseProject(ReleaseProjectBean releaseProjectBean) {
        this.releaseId = releaseProjectBean.getReleaseId();
        this.projectId = releaseProjectBean.getProjectId();
        this.branch = releaseProjectBean.getDeployBranch();
    }


    public static ReleaseProject valueOf(ReleaseProjectBean releaseProjectBean) {
        return new ReleaseProject(releaseProjectBean);
    }
}
