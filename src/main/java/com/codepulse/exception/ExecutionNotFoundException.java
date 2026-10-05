package com.codepulse.exception;

public class ExecutionNotFoundException extends RuntimeException {

    public ExecutionNotFoundException(Long id) {
        super("Execution not found with id: " + id);
    }
}