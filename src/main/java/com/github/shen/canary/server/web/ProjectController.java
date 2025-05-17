package com.github.shen.canary.server.web;

import com.github.shen.canary.server.entity.Project;
import com.github.shen.canary.server.repository.ProjectRepository;
import com.github.shen.canary.server.web.request.ProjectSearch;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@AllArgsConstructor
public class ProjectController {

    private ProjectRepository projectRepository;

    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        Project savedProject = projectRepository.save(project);
        return ResponseEntity.ok(savedProject);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectRepository.get(id));
    }

    @PostMapping("/search")
    public ResponseEntity<List<Project>> getProjects(@RequestBody ProjectSearch request) {
        return ResponseEntity.ok(projectRepository.get(request));
    }
}