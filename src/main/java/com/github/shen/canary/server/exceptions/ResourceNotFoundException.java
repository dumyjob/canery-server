package com.github.shen.canary.server.exceptions;


public class ResourceNotFoundException extends RuntimeException{

    public ResourceNotFoundException(final String message) {
        super(message);
    }
}
