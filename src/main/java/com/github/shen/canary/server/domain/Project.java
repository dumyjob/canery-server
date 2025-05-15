package com.github.shen.canary.server.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.Map;

@Entity
@Table(name = "projects")
@Data
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String name;
    private String gitRepo;       // Git仓库地址
    private String branch;        // 分支名称
    
    @Column(columnDefinition = "JSON")
    private String cloudConfig;    // 云资源模板（ROS或Terraform）
    
    @ElementCollection
    private Map<String, String> envVars;  // 环境变量
}