package com.github.shen.canary.server.web;

import com.github.shen.canary.server.entity.Deployment;
import com.github.shen.canary.server.entity.Project;
import com.github.shen.canary.server.repository.DeploymentRepository;
import com.github.shen.canary.server.repository.ProjectRepository;
import com.github.shen.canary.server.service.DeployService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/deployments")
@AllArgsConstructor
public class DeploymentController {


    private ProjectRepository projectRepository;
    private DeploymentRepository deploymentRepository;

    private DeployService deployService;

    @PostMapping("/{projectId}/deploy")
    public ResponseEntity<Deployment> deploy(
        @PathVariable String projectId,
        @RequestParam String env) {  // 环境参数（prod/test）

        Project project = projectRepository.get(projectId);
        Deployment deployment = new Deployment();
        deployment.setId(UUID.randomUUID().toString());
        deployment.setStatus("PENDING");
        deploymentRepository.save(deployment);

        // 异步调用Python任务
        String taskId = deployService.deploySync(
            project.getCloudConfig(),
            env
        );
        deployment.setId(taskId);
        deploymentRepository.save(deployment);

        return ResponseEntity.accepted().body(deployment);
    }
}
