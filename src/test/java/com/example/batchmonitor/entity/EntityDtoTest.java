package com.example.batchmonitor.entity;

import com.example.batchmonitor.dto.BatchExecutionResponse;
import com.example.batchmonitor.dto.ConsoleOutput;
import com.example.batchmonitor.dto.ProgressUpdate;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EntityDtoTest {

    @Test
    void testBatchExecutionEntity() {
        // Test builder and getters/setters
        LocalDateTime now = LocalDateTime.now();
        BatchExecution execution = BatchExecution.builder()
                .id(1L)
                .scriptPath("/path/to/script.sh")
                .parameters("--param value")
                .startTime(now)
                .endTime(now.plusMinutes(5))
                .status(BatchExecution.ExecutionStatus.COMPLETED)
                .exitCode(0)
                .progress(100.0)
                .outputFilePath("/path/to/output.log")
                .errorMessage(null)
                .build();

        // Test getters
        assertEquals(1L, execution.getId());
        assertEquals("/path/to/script.sh", execution.getScriptPath());
        assertEquals("--param value", execution.getParameters());
        assertEquals(now, execution.getStartTime());
        assertEquals(now.plusMinutes(5), execution.getEndTime());
        assertEquals(BatchExecution.ExecutionStatus.COMPLETED, execution.getStatus());
        assertEquals(0, execution.getExitCode());
        assertEquals(100.0, execution.getProgress());
        assertEquals("/path/to/output.log", execution.getOutputFilePath());
        assertNull(execution.getErrorMessage());
        assertNotNull(execution.getLogs());
        assertTrue(execution.getLogs().isEmpty());

        // Test setters
        execution.setScriptPath("/new/path.sh");
        execution.setParameters("--new param");
        execution.setProgress(99.9);

        assertEquals("/new/path.sh", execution.getScriptPath());
        assertEquals("--new param", execution.getParameters());
        assertEquals(99.9, execution.getProgress());
    }

    @Test
    void testExecutionLogEntity() {
        // Create parent execution
        BatchExecution execution = BatchExecution.builder()
                .id(1L)
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .build();

        // Test builder and getters/setters
        LocalDateTime now = LocalDateTime.now();
        ExecutionLog log = ExecutionLog.builder()
                .id(100L)
                .batchExecution(execution)
                .message("Test log message")
                .timestamp(now)
                .logType(ExecutionLog.LogType.STDOUT)
                .build();

        // Test getters
        assertEquals(100L, log.getId());
        assertEquals(execution, log.getBatchExecution());
        assertEquals("Test log message", log.getMessage());
        assertEquals(now, log.getTimestamp());
        assertEquals(ExecutionLog.LogType.STDOUT, log.getLogType());

        // Test setters
        log.setMessage("Updated message");
        log.setLogType(ExecutionLog.LogType.STDERR);

        assertEquals("Updated message", log.getMessage());
        assertEquals(ExecutionLog.LogType.STDERR, log.getLogType());
    }

    @Test
    void testBatchExecutionResponseDto() {
        // Test builder and conversion from entity
        LocalDateTime now = LocalDateTime.now();
        BatchExecution execution = BatchExecution.builder()
                .id(1L)
                .scriptPath("/path/to/script.sh")
                .parameters("--param value")
                .startTime(now)
                .endTime(now.plusMinutes(5))
                .status(BatchExecution.ExecutionStatus.COMPLETED)
                .exitCode(0)
                .progress(100.0)
                .errorMessage(null)
                .build();

        // Test fromEntity conversion
        BatchExecutionResponse response = BatchExecutionResponse.fromEntity(execution);

        assertEquals(1L, response.getId());
        assertEquals("/path/to/script.sh", response.getScriptPath());
        assertEquals("--param value", response.getParameters());
        assertEquals(now, response.getStartTime());
        assertEquals(now.plusMinutes(5), response.getEndTime());
        assertEquals(BatchExecution.ExecutionStatus.COMPLETED, response.getStatus());
        assertEquals(0, response.getExitCode());
        assertEquals(100.0, response.getProgress());
        assertNull(response.getErrorMessage());

        // Test direct builder
        BatchExecutionResponse directResponse = BatchExecutionResponse.builder()
                .id(2L)
                .scriptPath("/another/script.sh")
                .progress(50.0)
                .build();

        assertEquals(2L, directResponse.getId());
        assertEquals("/another/script.sh", directResponse.getScriptPath());
        assertEquals(50.0, directResponse.getProgress());

        // Test setters
        directResponse.setErrorMessage("Error occurred");
        assertEquals("Error occurred", directResponse.getErrorMessage());
    }

    @Test
    void testConsoleOutputDto() {
        // Test builder and conversion from entity
        LocalDateTime now = LocalDateTime.now();
        BatchExecution execution = BatchExecution.builder()
                .id(1L)
                .build();

        ExecutionLog log = ExecutionLog.builder()
                .id(100L)
                .batchExecution(execution)
                .message("Log message")
                .timestamp(now)
                .logType(ExecutionLog.LogType.STDOUT)
                .build();

        // Test fromLog conversion
        ConsoleOutput output = ConsoleOutput.fromLog(log);

        assertEquals(1L, output.getExecutionId());
        assertEquals("Log message", output.getMessage());
        assertEquals(now, output.getTimestamp());
        assertEquals(ConsoleOutput.OutputType.STDOUT, output.getType());

        // Test stderr conversion
        log.setLogType(ExecutionLog.LogType.STDERR);
        ConsoleOutput errorOutput = ConsoleOutput.fromLog(log);
        assertEquals(ConsoleOutput.OutputType.STDERR, errorOutput.getType());

        // Test direct builder
        ConsoleOutput directOutput = ConsoleOutput.builder()
                .executionId(2L)
                .message("Direct console output")
                .timestamp(now.plusMinutes(1))
                .type(ConsoleOutput.OutputType.STDOUT)
                .build();

        assertEquals(2L, directOutput.getExecutionId());
        assertEquals("Direct console output", directOutput.getMessage());
        assertEquals(now.plusMinutes(1), directOutput.getTimestamp());
        assertEquals(ConsoleOutput.OutputType.STDOUT, directOutput.getType());
    }

    @Test
    void testProgressUpdateDto() {
        // Test builder
        ProgressUpdate update = ProgressUpdate.builder()
                .executionId(1L)
                .progress(75.5)
                .status("RUNNING")
                .build();

        assertEquals(1L, update.getExecutionId());
        assertEquals(75.5, update.getProgress());
        assertEquals("RUNNING", update.getStatus());

        // Test setters
        update.setProgress(80.0);
        update.setStatus("COMPLETED");

        assertEquals(80.0, update.getProgress());
        assertEquals("COMPLETED", update.getStatus());
    }
}
