package com.github.shen.canary.server.service;

import com.github.shen.canary.server.entity.DeploymentStatus;

public interface DeployService {
    /**
     * 触发部署任务（异步）
     *
     * @param projectId 项目ID
     * @param branch 項目发布分支
     * @param env       部署环境（prod/test）
     * @return 部署任务ID
     */
    String deploySync(Long projectId, String branch, String env);
    
    /**
     * 查询部署状态
     * @param taskId 任务ID
     * @return 最新状态
     */
    DeploymentStatus getDeployStatus(String taskId);
}