package com.github.shen.canary.server.repository;

import com.github.shen.canary.server.domain.Deployment;

import java.util.Optional;

public interface DeploymentRepository {

    void save(Deployment deployment);

    Optional<Deployment> findById(String deploymentId);

}
