package com.codepulse.dto;

import jakarta.validation.constraints.NotBlank;

public class ExecutionRequest {

    @NotBlank(message = "Code cannot be empty")
    private String code;

    private String input;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}