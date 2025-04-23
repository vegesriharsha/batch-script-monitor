package com.example.batchmonitor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BatchExecutionRequest {

    @NotBlank(message = "Script name is required")
    private String scriptName;

    private String parameters;
}
