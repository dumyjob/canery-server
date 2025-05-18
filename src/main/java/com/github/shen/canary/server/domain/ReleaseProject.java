package com.github.shen.canary.server.domain;

import com.github.shen.canary.server.entity.ReleaseProjectBean;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReleaseProject {

    private Long releaseId;
    private Long projectId;
    private String branch;

    private ReleaseProject(ReleaseProjectBean releaseProjectBean) {
        this.releaseId = releaseProjectBean.getReleaseId();
        this.projectId = releaseProjectBean.getProjectId();
        this.branch = releaseProjectBean.getDeployBranch();
    }


    public static ReleaseProject valueOf(ReleaseProjectBean releaseProjectBean) {
        return new ReleaseProject(releaseProjectBean);
    }
}
