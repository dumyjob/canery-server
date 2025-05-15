package com.github.shen.canary.server.repository.impl;

import com.github.shen.canary.server.domain.Deployment;
import com.github.shen.canary.server.repository.DeploymentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
public class DeploymentRepositoryImpl implements DeploymentRepository {


    @Override
    public void save(final Deployment deployment) {

    }


    @Override
    public Optional<Deployment> findById(final String deploymentId) {
        return Optional.empty();
    }
}
