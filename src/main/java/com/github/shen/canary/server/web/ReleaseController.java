package com.github.shen.canary.server.web;

import com.github.shen.canary.server.domain.Release;
import com.github.shen.canary.server.repository.ReleaseRepository;
import com.github.shen.canary.server.service.DeployService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/releases")
@AllArgsConstructor
public class ReleaseController {

    private final ReleaseRepository releaseRepository;
    private final DeployService deployService;

    @PostMapping("/deploy/{releaseId")
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
