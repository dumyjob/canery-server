package com.github.shen.canary.server.exceptions;


import com.fasterxml.jackson.core.JsonProcessingException;

public class DeploymentException extends RuntimeException{

    public DeploymentException(final JsonProcessingException message) {
        super(message);
    }
}
