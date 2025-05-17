package com.github.shen.canary.server.web;

import com.github.shen.canary.server.repository.DeploymentRepository;
import com.github.shen.canary.server.repository.ProjectRepository;
import com.github.shen.canary.server.service.DeployService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deployments")
@AllArgsConstructor
public class DeploymentController {

    private ProjectRepository projectRepository;
    private DeploymentRepository deploymentRepository;

    private DeployService deployService;

    // 通过发布单触发发布,不是直接发布

//    @PostMapping("/{projectId}/deploy")
//    public ResponseEntity<Deployment> deploy(
//        @PathVariable Long projectId,
//        @RequestParam String env) {  // 环境参数（prod/test）
//
//        Project project = projectRepository.get(projectId);
//        Deployment deployment = new Deployment();
//        deployment.setId(UUID.randomUUID().toString());
//        deployment.setStatus("PENDING");
//        deploymentRepository.save(deployment);
//
//        // 异步调用Python任务
//        String taskId = deployService.deploySync(
//            (project.getCloudConfig),
//                env);
//        deployment.setId(taskId);
//        deploymentRepository.save(deployment);
//
//        return ResponseEntity.accepted().body(deployment);
//    }
}
