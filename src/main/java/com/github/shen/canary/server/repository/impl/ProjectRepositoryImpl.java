package com.github.shen.canary.server.repository.impl;

import com.github.shen.canary.server.dao.ProjectMapper;
import com.github.shen.canary.server.entity.Project;
import com.github.shen.canary.server.exceptions.DatabaseException;
import com.github.shen.canary.server.repository.ProjectRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepository  {

    private final ProjectMapper projectMapper;

    @Override
    public Project save(final Project project) {
        int rows = projectMapper.insertSelective(project);
        if (rows != 1) {
            throw new DatabaseException("项目配置异常");
        }

        return project;
    }

    @Override
    public Project get(final Long id) {
        return projectMapper.selectByPrimaryKey(id);
    }
}
