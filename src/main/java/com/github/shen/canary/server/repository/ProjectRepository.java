package com.github.shen.canary.server.repository;

import com.github.shen.canary.server.entity.Project;
import com.github.shen.canary.server.web.request.ProjectSearch;

import java.util.List;

public interface ProjectRepository {

    Project save(Project project);

    Project get(Long id);

    List<Project> get(ProjectSearch request);

    void remove(Long id);

    Project update(Project project);
}
