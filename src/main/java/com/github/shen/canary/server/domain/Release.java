package com.github.shen.canary.server.domain;

import com.github.shen.canary.server.entity.Deployment;
import com.github.shen.canary.server.entity.ReleaseOrderBean;
import com.github.shen.canary.server.entity.ReleaseProjectBean;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class Release {

    private final Long id;
    private final String releaseDesc;
    private final ReleaseType releaseType;

    private final String env;

    private final List<ReleaseProject> releaseProjects;


    private Release(ReleaseOrderBean releaseOrderBean, List<ReleaseProjectBean> releaseProjects) {
        this.id = releaseOrderBean.getId();
        this.releaseDesc = releaseOrderBean.getReleaseName();
        this.releaseType = ReleaseType.valueOf(releaseOrderBean.getReleaseType());
        this.env = releaseOrderBean.getEnv();
        if (CollectionUtils.isEmpty(releaseProjects)) {
            throw new IllegalArgumentException("发布项目不能为空");
        }

        this.releaseProjects = releaseProjects.stream()
                .map(ReleaseProject::valueOf)
                .collect(Collectors.toList());
    }

    public static Release valueOf(ReleaseOrderBean releaseOrderBean, List<ReleaseProjectBean> releaseProjects) {
        return new Release(releaseOrderBean, releaseProjects);
    }

    public List<Deployment> deploys() {
        return null;
    }
}
