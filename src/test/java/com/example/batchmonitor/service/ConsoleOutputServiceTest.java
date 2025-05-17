package com.example.batchmonitor.service;

import com.example.batchmonitor.dto.ConsoleOutput;
import com.example.batchmonitor.dto.ProgressUpdate;
import com.example.batchmonitor.entity.BatchExecution;
import com.example.batchmonitor.entity.ExecutionLog;
import com.example.batchmonitor.repository.ExecutionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsoleOutputServiceTest {

    @Mock
    private ExecutionLogRepository logRepository;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private ProgressParserService progressParserService;

    @InjectMocks
    private ConsoleOutputService consoleOutputService;

    @Test
    void processStandardOutput_ShouldSaveLogAndSendUpdate() {
        // Arrange
        BatchExecution execution = BatchExecution.builder()
                .id(1L)
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .progress(25.0)
                .build();

        String line = "Processing data...";

        when(progressParserService.parseProgress(line)).thenReturn(null); // No progress update in this line

        // Act
        consoleOutputService.processStandardOutput(execution, line);

        // Assert
        // Verify log entry is saved
        ArgumentCaptor<ExecutionLog> logCaptor = ArgumentCaptor.forClass(ExecutionLog.class);
        verify(logRepository).save(logCaptor.capture());

        ExecutionLog capturedLog = logCaptor.getValue();
        assertEquals(execution, capturedLog.getBatchExecution());
        assertEquals(line, capturedLog.getMessage());
        assertEquals(ExecutionLog.LogType.STDOUT, capturedLog.getLogType());
        assertNotNull(capturedLog.getTimestamp());

        // Verify WebSocket update is sent
        ArgumentCaptor<ConsoleOutput> outputCaptor = ArgumentCaptor.forClass(ConsoleOutput.class);
        verify(webSocketService).sendConsoleOutput(outputCaptor.capture());

        ConsoleOutput capturedOutput = outputCaptor.getValue();
        assertEquals(1L, capturedOutput.getExecutionId());
        assertEquals(line, capturedOutput.getMessage());
        assertEquals(ConsoleOutput.OutputType.STDOUT, capturedOutput.getType());

        // Verify progress parser was called
        verify(progressParserService).parseProgress(line);

        // Verify no progress update sent (since parseProgress returned null)
        verify(webSocketService, never()).sendProgressUpdate(any(ProgressUpdate.class));
    }

    @Test
    void processStandardOutput_WithProgressInfo_ShouldUpdateProgress() {
        // Arrange
        BatchExecution execution = BatchExecution.builder()
                .id(1L)
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .progress(25.0)
                .build();

        String line = "Progress: 50%";

        when(progressParserService.parseProgress(line)).thenReturn(50.0); // Line contains progress update

        // Act
        consoleOutputService.processStandardOutput(execution, line);

        // Assert
        // Verify log entry is saved
        verify(logRepository).save(any(ExecutionLog.class));

        // Verify WebSocket console output is sent
        verify(webSocketService).sendConsoleOutput(any(ConsoleOutput.class));

        // Verify progress parser was called
        verify(progressParserService).parseProgress(line);

        // Verify progress update is sent
        ArgumentCaptor<ProgressUpdate> progressCaptor = ArgumentCaptor.forClass(ProgressUpdate.class);
        verify(webSocketService).sendProgressUpdate(progressCaptor.capture());

        ProgressUpdate capturedProgress = progressCaptor.getValue();
        assertEquals(1L, capturedProgress.getExecutionId());
        assertEquals(50.0, capturedProgress.getProgress());
        assertEquals("RUNNING", capturedProgress.getStatus());

        // Verify execution was updated
        assertEquals(50.0, execution.getProgress());
    }

    @Test
    void processErrorOutput_ShouldSaveLogAndSendUpdate() {
        // Arrange
        BatchExecution execution = BatchExecution.builder()
                .id(1L)
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .build();

        String errorLine = "Error occurred during processing";

        // Act
        consoleOutputService.processErrorOutput(execution, errorLine);

        // Assert
        // Verify log entry is saved
        ArgumentCaptor<ExecutionLog> logCaptor = ArgumentCaptor.forClass(ExecutionLog.class);
        verify(logRepository).save(logCaptor.capture());

        ExecutionLog capturedLog = logCaptor.getValue();
        assertEquals(execution, capturedLog.getBatchExecution());
        assertEquals(errorLine, capturedLog.getMessage());
        assertEquals(ExecutionLog.LogType.STDERR, capturedLog.getLogType());
        assertNotNull(capturedLog.getTimestamp());

        // Verify WebSocket update is sent
        ArgumentCaptor<ConsoleOutput> outputCaptor = ArgumentCaptor.forClass(ConsoleOutput.class);
        verify(webSocketService).sendConsoleOutput(outputCaptor.capture());

        ConsoleOutput capturedOutput = outputCaptor.getValue();
        assertEquals(1L, capturedOutput.getExecutionId());
        assertEquals(errorLine, capturedOutput.getMessage());
        assertEquals(ConsoleOutput.OutputType.STDERR, capturedOutput.getType());
    }

    @Test
    void logSystemMessage_ShouldSaveLog() {
        // Arrange
        BatchExecution execution = BatchExecution.builder()
                .id(1L)
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .build();

        String message = "System message: execution started";

        // Act
        consoleOutputService.logSystemMessage(execution, message);

        // Assert
        // Verify log entry is saved
        ArgumentCaptor<ExecutionLog> logCaptor = ArgumentCaptor.forClass(ExecutionLog.class);
        verify(logRepository).save(logCaptor.capture());

        ExecutionLog capturedLog = logCaptor.getValue();
        assertEquals(execution, capturedLog.getBatchExecution());
        assertEquals(message, capturedLog.getMessage());
        assertEquals(ExecutionLog.LogType.SYSTEM, capturedLog.getLogType());
        assertNotNull(capturedLog.getTimestamp());

        // Verify no WebSocket update is sent for system messages
        verify(webSocketService, never()).sendConsoleOutput(any(ConsoleOutput.class));
    }

    @Test
    void getConsoleOutput_ShouldReturnNonSystemLogs() {
        // Arrange
        BatchExecution execution = BatchExecution.builder()
                .id(1L)
                .status(BatchExecution.ExecutionStatus.COMPLETED)
                .build();

        LocalDateTime now = LocalDateTime.now();

        List<ExecutionLog> logs = Arrays.asList(
                ExecutionLog.builder()
                        .id(1L)
                        .batchExecution(execution)
                        .message("Starting execution")
                        .timestamp(now.minusMinutes(5))
                        .logType(ExecutionLog.LogType.STDOUT)
                        .build(),
                ExecutionLog.builder()
                        .id(2L)
                        .batchExecution(execution)
                        .message("System: Initializing")
                        .timestamp(now.minusMinutes(4))
                        .logType(ExecutionLog.LogType.SYSTEM)  // System log should be filtered out
                        .build(),
                ExecutionLog.builder()
                        .id(3L)
                        .batchExecution(execution)
                        .message("Error occurred")
                        .timestamp(now.minusMinutes(3))
                        .logType(ExecutionLog.LogType.STDERR)
                        .build()
        );

        when(logRepository.findByBatchExecutionIdOrderByTimestampAsc(1L)).thenReturn(logs);

        // Act
        List<ConsoleOutput> result = consoleOutputService.getConsoleOutput(1L);

        // Assert
        assertEquals(2, result.size()); // Only non-system logs should be returned

        assertEquals("Starting execution", result.get(0).getMessage());
        assertEquals(ConsoleOutput.OutputType.STDOUT, result.get(0).getType());

        assertEquals("Error occurred", result.get(1).getMessage());
        assertEquals(ConsoleOutput.OutputType.STDERR, result.get(1).getType());

        verify(logRepository).findByBatchExecutionIdOrderByTimestampAsc(1L);
    }
}
