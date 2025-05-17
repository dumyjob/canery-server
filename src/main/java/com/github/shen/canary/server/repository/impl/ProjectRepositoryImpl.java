package com.github.shen.canary.server.repository.impl;

import com.github.shen.canary.server.entity.Project;
import com.github.shen.canary.server.repository.ProjectRepository;
import org.springframework.stereotype.Component;

@Component
public class ProjectRepositoryImpl implements ProjectRepository  {


    @Override
    public Project save(final Project project) {
        return null;
    }

    @Override
    public Project get(final String id) {
        return null;
    }
}
