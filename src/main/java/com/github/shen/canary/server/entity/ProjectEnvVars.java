package com.github.shen.canary.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;

import java.util.Map;

@Entity
@Table(name = "project_env_vars")
@Data
public class ProjectEnvVars {

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "var_key")
    private String varKey;
    @Column(name = "var_value")
    private String varValue;
}