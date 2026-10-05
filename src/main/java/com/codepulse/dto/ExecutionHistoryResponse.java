package com.codepulse.dto;

import java.time.LocalDateTime;

public class ExecutionHistoryResponse {

    private Long id;
    private String status;
    private String output;
    private Long executionTimeMs;
    private LocalDateTime createdAt;

    public ExecutionHistoryResponse(
            Long id,
            String status,
            String output,
            Long executionTimeMs,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.status = status;
        this.output = output;
        this.executionTimeMs = executionTimeMs;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}