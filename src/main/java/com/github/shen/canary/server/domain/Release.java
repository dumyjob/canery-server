package com.github.shen.canary.server.domain;

import com.github.shen.canary.server.entity.ReleaseOrderBean;
import com.github.shen.canary.server.entity.ReleaseProjectBean;
import com.github.shen.canary.server.web.request.ReleaseOrderRequest;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public class Release {

    private final Long id;
    private final String releaseDesc;
    private final ReleaseType releaseType;

    private final String env;

    private final TrafficRule trafficRule;

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

    private Release(ReleaseOrderRequest release) {
        this.id = release.getReleaseId();
        this.releaseDesc = release.getReleaseDesc();
        this.releaseType = ReleaseType.valueOf(release.getReleaseType());
        this.env = release.getEnv();
        if (CollectionUtils.isEmpty(release.getProjects())) {
            throw new IllegalArgumentException("发布项目不能为空");
        }
        this.releaseProjects = release.getProjects();
    }

    public static Release valueOf(ReleaseOrderBean releaseOrderBean, List<ReleaseProjectBean> releaseProjects) {
        return new Release(releaseOrderBean, releaseProjects);
    }

    public static Release valueOf(ReleaseOrderRequest release) {
        return new Release(release);
    }

    public static List<Release> mapValue(List<ReleaseOrderBean> releaseOrderBeans, List<ReleaseProjectBean> projects) {
        if (CollectionUtils.isEmpty(releaseOrderBeans)) {
            return Collections.emptyList();
        }

        // project
        Map<Long, List<ReleaseProjectBean>> mapValue = projects.stream()
                .collect(Collectors.groupingBy(
                        ReleaseProjectBean::getReleaseId,
                        Collectors.toList()
                ));

        return releaseOrderBeans.stream()
                .map(releaseOrderBean -> new Release(releaseOrderBean, mapValue.get(releaseOrderBean.getId()))
                )
                .collect(Collectors.toList());
    }

    public ReleaseOrderBean releaseOrderBean() {
        ReleaseOrderBean releaseOrderBean = new ReleaseOrderBean();
        if (!Objects.isNull(this.id)) {
            releaseOrderBean.setId(id);
        }
        releaseOrderBean.setReleaseName(this.releaseDesc);
        releaseOrderBean.setReleaseType(this.releaseType.name());
        releaseOrderBean.setEnv(this.env);
        releaseOrderBean.setTrafficRule(this);

        return releaseOrderBean;
    }

    public List<ReleaseProjectBean> releaseProjectBeans(final Long releaseId) {
        if (CollectionUtils.isEmpty(this.releaseProjects)) {
            return Collections.emptyList();
        }

        return this.releaseProjects
                .stream()
                .map(releaseProject -> {
                    ReleaseProjectBean releaseProjectBean = new ReleaseProjectBean();
                    releaseProjectBean.setReleaseId(releaseId);
                    releaseProjectBean.setProjectId(releaseProject.getProjectId());
                    releaseProjectBean.setDeployBranch(releaseProject.getBranch());

                    return releaseProjectBean;
                })
                .collect(Collectors.toList());


    }
}
