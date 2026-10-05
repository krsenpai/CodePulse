package com.codepulse.dto;

public class ExecutionResponse {

    private String status;
    private String output;
    private Long executionTimeMs;

    public ExecutionResponse(
            String status,
            String output,
            Long executionTimeMs
    ) {
        this.status = status;
        this.output = output;
        this.executionTimeMs = executionTimeMs;
    }

    public String getStatus() {
        return status;
    }

    public String getOutput() {
        return output;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
}