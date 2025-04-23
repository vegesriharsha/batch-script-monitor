package com.example.batchmonitor.dto;

import com.example.batchmonitor.entity.BatchExecution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchExecutionResponse {

    private Long id;
    private String scriptPath;
    private String parameters;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BatchExecution.ExecutionStatus status;
    private Integer exitCode;
    private Double progress;
    private String result;
    private String errorMessage;

    public static BatchExecutionResponse fromEntity(BatchExecution execution) {
        return BatchExecutionResponse.builder()
                .id(execution.getId())
                .scriptPath(execution.getScriptPath())
                .parameters(execution.getParameters())
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .status(execution.getStatus())
                .exitCode(execution.getExitCode())
                .progress(execution.getProgress())
                .errorMessage(execution.getErrorMessage())
                .build();
    }
}
