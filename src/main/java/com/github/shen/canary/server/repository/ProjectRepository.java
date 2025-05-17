package com.github.shen.canary.server.repository;

import com.github.shen.canary.server.entity.Project;

public interface ProjectRepository {

    Project save(Project project);

    Project get(Long id);

}
