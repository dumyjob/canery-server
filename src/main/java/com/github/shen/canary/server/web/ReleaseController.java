package com.github.shen.canary.server.web;

import com.github.shen.canary.server.domain.Release;
import com.github.shen.canary.server.repository.ReleaseRepository;
import com.github.shen.canary.server.service.DeployService;
import com.github.shen.canary.server.web.request.ReleaseOrderRequest;
import com.github.shen.canary.server.web.request.ReleaseSearch;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/releases")
@AllArgsConstructor
public class ReleaseController {

    private final ReleaseRepository releaseRepository;
    private final DeployService deployService;

    @PostMapping
    public ResponseEntity<Release> createRelease(@RequestBody ReleaseOrderRequest release) {
        Release releaseOrder = releaseRepository.save(Release.valueOf(release));
        return ResponseEntity.ok(releaseOrder);
    }

    @PutMapping
    public ResponseEntity<Release> updateRelease(@RequestBody ReleaseOrderRequest release) {
        Release releaseOrder = releaseRepository.update(Release.valueOf(release));
        return ResponseEntity.ok(releaseOrder);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Release> getProject(@PathVariable Long id) {
        Release releaseOrder = releaseRepository.get(id);
        return ResponseEntity.ok(releaseOrder);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteProject(@PathVariable Long id) {
        releaseRepository.remove(id);
        return ResponseEntity.ok(Boolean.TRUE);
    }

    @PostMapping("/search")
    public ResponseEntity<List<Release>> getProjects(@RequestBody ReleaseSearch request) {
        return ResponseEntity.ok(releaseRepository.get(request));
    }

    @PostMapping("/deploy/{releaseId}")
    public ResponseEntity<Boolean> deploy(@PathVariable("releaseId") Long releaseId) {
        // 根据发布单生成部署任务
        final Release release = releaseRepository.get(releaseId);

        release.getReleaseProjects()
                .forEach(releaseProject -> {
                    deployService.deploySync(releaseProject.getProjectId(),
                            releaseProject.getBranch(),
                            release.getEnv());
                });

        return ResponseEntity.accepted().body(Boolean.TRUE);
    }

}
