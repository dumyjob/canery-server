package com.github.shen.canary.server.repository;

import com.github.shen.canary.server.domain.Project;

public interface ProjectRepository {

    Project save(Project project);

    Project get(String id);

}
