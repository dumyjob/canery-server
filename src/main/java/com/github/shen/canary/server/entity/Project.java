package com.github.shen.canary.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "projects")
@Data
public class Project extends DataBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "JDBC")
    private Long id;

    // 项目名称(zh/en)
    private String name;
    // 项目描述
    private String description;

    // Git仓库地址
    @Column(name = "git_repos")
    private String gitRepos;

    @Column(name = "web_hook")
    private String webhook;

    // 项目类型(boot-jar/dubbo)
    @Column(name = "project_type")
    private String projectType;

    // 云资源模板（ROS或Terraform）
    @Column(columnDefinition = "JSON", name = "cloud_config")
    private String cloudConfig;

    // 环境变量
    @Transient
    private Map<String, String> envVars;

    // cpu数量
    private Integer cpu;
    // 内存大小
    private Integer memory;
    // pod数量
    private Integer pods;

    // jvm参数
    @Column(name = "jvm_args")
    private String jvmArgs;

    public List<ProjectEnvVars> envVarsBean(Long projectId) {
        if (CollectionUtils.isEmpty(envVars)) {
            return Collections.emptyList();
        }

        return envVars.entrySet()
                .stream().map(entry -> {
                    ProjectEnvVars projectEnvVars = new ProjectEnvVars();
                    projectEnvVars.setProjectId(this.id);
                    projectEnvVars.setVarKey(entry.getKey());
                    projectEnvVars.setVarValue(entry.getValue());

                    return projectEnvVars;
                })
                .collect(Collectors.toList());

    }
}