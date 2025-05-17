package com.github.shen.canary.server.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;

import javax.persistence.*;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "projects")
@Data
public class Project extends DataBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 项目名称(zh/en)
    private String name;
    // 项目描述
    private String description;

    // Git仓库地址
    @Column(name = "git_repos")
    private String gitRepos;

    @Column(name = "web_hook")
    private String webHook;

    // 项目类型(boot-jar/dubbo)
    @Column(name = "project_type")
    private String projectType;

    // 云资源模板（ROS或Terraform）
    @Column(columnDefinition = "JSON", name = "cloud_config")
    private String cloudConfig;

    // 环境变量
    @ElementCollection
    @Column(name = "env_vars")
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
}