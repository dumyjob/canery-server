package com.github.shen.canary.server.web.request;

import com.github.shen.canary.server.domain.ReleaseProject;
import lombok.Data;

import java.util.List;

@Data
public class ReleaseOrderRequest {

    private Long id;

    private String releaseDesc;
    private String releaseType;
    private String env;

    private String grayVersion;

    private String trafficPolicy;

    private List<ReleaseProject> projects;
}
