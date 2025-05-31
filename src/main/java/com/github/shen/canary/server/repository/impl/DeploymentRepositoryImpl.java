package com.github.shen.canary.server.repository.impl;

import com.github.shen.canary.server.dao.DeploymentMapper;
import com.github.shen.canary.server.entity.Deployment;
import com.github.shen.canary.server.exceptions.DatabaseException;
import com.github.shen.canary.server.repository.DeploymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
@AllArgsConstructor
public class DeploymentRepositoryImpl implements DeploymentRepository {

    private final DeploymentMapper deploymentMapper;

    @Override
    public void save(final Deployment deployment) {
        int rows = deploymentMapper.insertSelective(deployment);
        if (rows != 1) {
            throw new DatabaseException("生成部署任务失败");
        }
    }


    @Override
    public Optional<Deployment> findById(final String deploymentId) {
        Deployment deployment = deploymentMapper.selectByPrimaryKey(deploymentId);
        return Optional.of(deployment);
    }

    @Override
    public void update(Deployment deployment) {
        int rows = deploymentMapper.updateByPrimaryKeySelective(deployment);
        if (rows != 1) {
            throw new DatabaseException("更新部署任务失败");
        }
    }
}
