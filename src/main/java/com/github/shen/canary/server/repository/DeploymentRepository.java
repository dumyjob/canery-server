package com.github.shen.canary.server.repository;

import com.github.shen.canary.server.entity.Deployment;

import java.util.Optional;

public interface DeploymentRepository {

    void save(Deployment deployment);

    Optional<Deployment> findById(String deploymentId);

    void update(Deployment deployment);
}
