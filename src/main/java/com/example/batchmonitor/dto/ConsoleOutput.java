package com.example.batchmonitor.dto;

import com.example.batchmonitor.entity.ExecutionLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsoleOutput {

    private Long executionId;
    private String message;
    private LocalDateTime timestamp;
    private OutputType type;

    public enum OutputType {
        STDOUT, STDERR
    }

    public static ConsoleOutput fromLog(ExecutionLog log) {
        return ConsoleOutput.builder()
                .executionId(log.getBatchExecution().getId())
                .message(log.getMessage())
                .timestamp(log.getTimestamp())
                .type(log.getLogType() == ExecutionLog.LogType.STDOUT
                        ? OutputType.STDOUT
                        : OutputType.STDERR)
                .build();
    }
}
