package com.codepulse.service;

import com.codepulse.dto.ExecutionHistoryResponse;
import com.codepulse.dto.ExecutionRequest;
import com.codepulse.dto.ExecutionResponse;
import com.codepulse.entity.Execution;
import com.codepulse.exception.ExecutionNotFoundException;
import com.codepulse.repository.ExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import com.codepulse.entity.User;
import com.codepulse.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class ExecutionService {

    private final ExecutionRepository executionRepository;
    private final DockerExecutionService dockerExecutionService;
    private final UserRepository userRepository;

    public ExecutionService(
            ExecutionRepository executionRepository,
            DockerExecutionService dockerExecutionService,
            UserRepository userRepository
    ) {
        this.executionRepository = executionRepository;
        this.dockerExecutionService = dockerExecutionService;
        this.userRepository = userRepository;
    }

    public ExecutionResponse execute(ExecutionRequest request) {

        long startTime = System.currentTimeMillis();

        try {
            DockerExecutionService.DockerResult result =
                    dockerExecutionService.execute(
                            request.getCode(),
                            request.getInput()
                    );

            long executionTime =
                    System.currentTimeMillis() - startTime;

            String status = result.getStatus();

            saveExecution(
                    request.getCode(),
                    request.getInput(),
                    result.getOutput(),
                    status,
                    executionTime
            );

            return new ExecutionResponse(
                    status,
                    result.getOutput(),
                    executionTime
            );

        } catch (Exception e) {

            long executionTime =
                    System.currentTimeMillis() - startTime;

            String errorMessage = e.getMessage();

            if (errorMessage == null) {
                errorMessage =
                        "An unexpected execution error occurred.";
            }

            saveExecution(
                    request.getCode(),
                    request.getInput(),
                    errorMessage,
                    "EXECUTION_ERROR",
                    executionTime
            );

            return new ExecutionResponse(
                    "EXECUTION_ERROR",
                    errorMessage,
                    executionTime
            );
        }
    }

    public Page<ExecutionHistoryResponse> getAllExecutions(Pageable pageable) {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return executionRepository
                .findByUserUsername(username, pageable)
                .map(execution -> new ExecutionHistoryResponse(
                        execution.getId(),
                        execution.getStatus(),
                        execution.getOutput(),
                        execution.getExecutionTimeMs(),
                        execution.getCreatedAt()
                ));
    }

    public ExecutionHistoryResponse getExecutionById(Long id) {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Execution execution = executionRepository
                .findByIdAndUserUsername(id, username)
                .orElseThrow(() ->
                        new ExecutionNotFoundException(id)
                );

        return new ExecutionHistoryResponse(
                execution.getId(),
                execution.getStatus(),
                execution.getOutput(),
                execution.getExecutionTimeMs(),
                execution.getCreatedAt()
        );
    }

    private void saveExecution(
            String code,
            String input,
            String output,
            String status,
            long executionTime
    ) {

        Execution execution = new Execution();

        execution.setCode(code);
        execution.setInput(input);
        execution.setOutput(output);
        execution.setStatus(status);
        execution.setExecutionTimeMs(executionTime);
        execution.setCreatedAt(LocalDateTime.now());

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        execution.setUser(user);

        executionRepository.save(execution);
    }
}