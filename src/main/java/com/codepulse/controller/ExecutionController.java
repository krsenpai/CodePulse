package com.codepulse.controller;

import com.codepulse.dto.ExecutionHistoryResponse;
import com.codepulse.dto.ExecutionPageResponse;
import com.codepulse.dto.ExecutionRequest;
import com.codepulse.dto.ExecutionResponse;
import com.codepulse.service.ExecutionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/execute")
    public ExecutionResponse execute(
            @Valid @RequestBody ExecutionRequest request
    ) {
        return executionService.execute(request);
    }

    @GetMapping("/executions")
    public ExecutionPageResponse getAllExecutions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        // Validate pagination
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be 0 or greater"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Size must be between 1 and 100"
            );
        }

        // Create pagination + sorting instructions
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        // Get paginated executions from service
        Page<ExecutionHistoryResponse> result =
                executionService.getAllExecutions(pageable);

        // Convert Spring Page into our own API response
        return new ExecutionPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @GetMapping("/executions/{id}")
    public ExecutionHistoryResponse getExecutionById(
            @PathVariable Long id
    ) {
        return executionService.getExecutionById(id);
    }
}