package com.github.shen.canary.server.repository.impl;

import com.github.shen.canary.server.dao.ProjectEnvVarsMapper;
import com.github.shen.canary.server.dao.ProjectMapper;
import com.github.shen.canary.server.entity.Project;
import com.github.shen.canary.server.entity.ProjectEnvVars;
import com.github.shen.canary.server.exceptions.DatabaseException;
import com.github.shen.canary.server.repository.ProjectRepository;
import com.github.shen.canary.server.web.request.ProjectSearch;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.weekend.Weekend;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepository {

    private final ProjectMapper projectMapper;
    private final ProjectEnvVarsMapper projectEnvVarsMapper;

    @Override
    public Project save(final Project project) {
        int rows = projectMapper.insertSelective(project);
        if (rows != 1) {
            throw new DatabaseException("项目配置异常");
        }

        project.envVarsBean(project.getId())
                .forEach(projectEnvVarsMapper::insert);

        return project;
    }

    @Override
    public Project get(final Long projectId) {
        Project project = projectMapper.selectByPrimaryKey(projectId);

        Weekend<ProjectEnvVars> varsExample = Weekend.of(ProjectEnvVars.class);
        varsExample.weekendCriteria()
                .andEqualTo(ProjectEnvVars::getProjectId, projectId);

        List<ProjectEnvVars> vars = projectEnvVarsMapper.selectByExample(varsExample);
        final Map<String, String> envVars = vars.stream()
                .collect(Collectors.toMap(
                        ProjectEnvVars::getVarKey,
                        ProjectEnvVars::getVarValue,
                        (oldVal, newVal) -> newVal  // 冲突时取新值
                ));
        project.setEnvVars(envVars);
        return project;
    }


    @Override
    public List<Project> get(ProjectSearch request) {
        Weekend<Project> projectExample = Weekend.of(Project.class);
        // 查询条件 todo

        return projectMapper.selectByExample(projectExample);
    }

    @Override
    public void remove(Long projectId) {
        projectMapper.deleteByPrimaryKey(projectId);

        Weekend<ProjectEnvVars> projectExample = Weekend.of(ProjectEnvVars.class);
        projectExample.weekendCriteria()
                .andEqualTo(ProjectEnvVars::getProjectId, projectId);
        projectEnvVarsMapper.deleteByExample(projectExample);
    }
}
