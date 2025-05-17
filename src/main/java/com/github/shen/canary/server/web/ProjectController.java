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

    @PutMapping
    public ResponseEntity<Project> updateProject(@RequestBody Project project) {
        Project updateProject = projectRepository.update(project);
        return ResponseEntity.ok(updateProject);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectRepository.get(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteProject(@PathVariable Long id) {
        projectRepository.remove(id);
        return ResponseEntity.ok(Boolean.TRUE);
    }

    @PostMapping("/search")
    public ResponseEntity<List<Project>> getProjects(@RequestBody ProjectSearch request) {
        return ResponseEntity.ok(projectRepository.get(request));
    }


}