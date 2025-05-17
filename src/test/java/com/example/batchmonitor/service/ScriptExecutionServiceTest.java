package com.example.batchmonitor.service;

import com.example.batchmonitor.dto.ProgressUpdate;
import com.example.batchmonitor.entity.BatchExecution;
import com.example.batchmonitor.exception.BatchExecutionException;
import com.example.batchmonitor.exception.ScriptExecutionException;
import com.example.batchmonitor.repository.BatchExecutionRepository;
import com.example.batchmonitor.util.ScriptUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScriptExecutionServiceTest {

    @Mock
    private BatchExecutionRepository executionRepository;

    @Mock
    private ConsoleOutputService consoleOutputService;

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private ScriptExecutionService scriptExecutionService;

    @TempDir
    Path tempDir;

    private Path logsDir;
    private BatchExecution testExecution;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temp logs directory
        logsDir = tempDir.resolve("logs");
        Files.createDirectories(logsDir);

        ReflectionTestUtils.setField(scriptExecutionService, "baseScriptsDir", tempDir.toString());
        ReflectionTestUtils.setField(scriptExecutionService, "executionTimeoutSeconds", 10);
        ReflectionTestUtils.setField(scriptExecutionService, "logsDirectory", logsDir.toString());
        ReflectionTestUtils.setField(scriptExecutionService, "keepLogCopy", true);

        // Create a test script file
        Path scriptFile = tempDir.resolve("test-script.sh");
        Files.writeString(scriptFile, "#!/bin/bash\necho 'Test script'\nexit 0");

        // Create a test execution
        testExecution = BatchExecution.builder()
                .id(1L)
                .scriptPath(scriptFile.toString())
                .parameters("--param value")
                .status(BatchExecution.ExecutionStatus.PENDING)
                .build();
    }

    @Test
    void executeScript_Success() throws Exception {
        // Using a real simple command for testing
        Path outputFile = logsDir.resolve("test-output.txt");
        Files.writeString(outputFile, "Test successful output");

        // Mock the script execution - we'll use a command that definitely exists on all platforms
        try (MockedStatic<ScriptUtils> scriptUtilsMock = Mockito.mockStatic(ScriptUtils.class)) {
            // Mock just enough to avoid running real processes but still test our flow
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                scriptUtilsMock.when(() -> ScriptUtils.buildCommand(anyString(), anyString()))
                        .thenReturn(List.of("cmd.exe", "/c", "echo", "Test output"));
            } else {
                scriptUtilsMock.when(() -> ScriptUtils.buildCommand(anyString(), anyString()))
                        .thenReturn(List.of("echo", "Test output"));
            }
            scriptUtilsMock.when(() -> ScriptUtils.readFileContent(anyString()))
                    .thenReturn("Test successful output");

            // Act
            CompletableFuture<String> future = scriptExecutionService.executeScript(testExecution);
            String result = future.get(); // Wait for completion

            // Assert
            assertEquals("Test successful output", result);
            assertEquals(BatchExecution.ExecutionStatus.COMPLETED, testExecution.getStatus());
            assertEquals(100.0, testExecution.getProgress());
            assertEquals(0, testExecution.getExitCode());
            assertNotNull(testExecution.getEndTime());

            // Verify repository interactions
            verify(executionRepository, times(2)).save(same(testExecution));

            // Verify websocket notifications
            verify(webSocketService).sendStatusUpdate(1L, "RUNNING");
            verify(webSocketService).sendStatusUpdate(1L, "COMPLETED");
            verify(webSocketService).sendProgressUpdate(argThat(update ->
                    update.getExecutionId() == 1L &&
                            update.getProgress() == 100.0 &&
                            "COMPLETED".equals(update.getStatus())
            ));

            // Verify console output service
            verify(consoleOutputService).logSystemMessage(same(testExecution),
                    contains("Script execution completed successfully"));
        }
    }

    @Test
    void executeScript_ProcessFails() throws Exception {
        // Using a command that should fail
        try (MockedStatic<ScriptUtils> scriptUtilsMock = Mockito.mockStatic(ScriptUtils.class)) {
            // Mock a failing command
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                scriptUtilsMock.when(() -> ScriptUtils.buildCommand(anyString(), anyString()))
                        .thenReturn(List.of("cmd.exe", "/c", "exit", "1"));
            } else {
                scriptUtilsMock.when(() -> ScriptUtils.buildCommand(anyString(), anyString()))
                        .thenReturn(List.of("sh", "-c", "exit 1"));
            }

            // Act
            CompletableFuture<String> future = scriptExecutionService.executeScript(testExecution);

            // Assert
            ScriptExecutionException exception = assertThrows(ScriptExecutionException.class,
                    () -> future.get()); // Should throw exception

            assertEquals(1, exception.getExitCode());

            assertEquals(BatchExecution.ExecutionStatus.FAILED, testExecution.getStatus());
            assertEquals(1, testExecution.getExitCode());
            assertNotNull(testExecution.getEndTime());
            assertNotNull(testExecution.getErrorMessage());

            // Verify repository interactions
            verify(executionRepository, times(2)).save(same(testExecution));

            // Verify websocket notifications
            verify(webSocketService).sendStatusUpdate(1L, "RUNNING");
            verify(webSocketService).sendStatusUpdate(1L, "FAILED");

            // Verify console output service
            verify(consoleOutputService).logSystemMessage(same(testExecution),
                    contains("Script execution failed"));
        }
    }

    @Test
    void updateExecutionSuccess_ShouldUpdateStatusAndNotifyClients() {
        // Arrange
        BatchExecution execution = BatchExecution.builder()
                .id(1L)
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .build();

        // Act
        scriptExecutionService.updateExecutionSuccess(execution);

        // Assert
        assertEquals(BatchExecution.ExecutionStatus.COMPLETED, execution.getStatus());
        assertEquals(100.0, execution.getProgress());
        assertNotNull(execution.getEndTime());

        verify(executionRepository).save(execution);
        verify(consoleOutputService).logSystemMessage(execution, "Script execution completed successfully");
        verify(webSocketService).sendStatusUpdate(1L, "COMPLETED");

        ArgumentCaptor<ProgressUpdate> updateCaptor = ArgumentCaptor.forClass(ProgressUpdate.class);
        verify(webSocketService).sendProgressUpdate(updateCaptor.capture());

        ProgressUpdate update = updateCaptor.getValue();
        assertEquals(1L, update.getExecutionId());
        assertEquals(100.0, update.getProgress());
        assertEquals("COMPLETED", update.getStatus());
    }

    @Test
    void updateExecutionFailure_ShouldUpdateStatusAndNotifyClients() {
        // Arrange
        BatchExecution execution = BatchExecution.builder()
                .id(1L)
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .build();

        String errorMessage = "Test error message";
        Integer exitCode = 2;

        // Act
        scriptExecutionService.updateExecutionFailure(execution, errorMessage, exitCode);

        // Assert
        assertEquals(BatchExecution.ExecutionStatus.FAILED, execution.getStatus());
        assertEquals(errorMessage, execution.getErrorMessage());
        assertEquals(exitCode, execution.getExitCode());
        assertNotNull(execution.getEndTime());

        verify(executionRepository).save(execution);
        verify(consoleOutputService).logSystemMessage(execution, "Script execution failed: " + errorMessage);
        verify(webSocketService).sendStatusUpdate(1L, "FAILED");
    }

    @Test
    void createLogFile_ShouldCreateValidFile() throws Exception {
        // Arrange
        BatchExecution execution = BatchExecution.builder()
                .id(1L)
                .build();

        // Use reflection to access private method
        Path logFile = (Path) ReflectionTestUtils.invokeMethod(
                scriptExecutionService,
                "createLogFile",
                execution);

        // Assert
        assertNotNull(logFile);
        assertTrue(Files.exists(logFile));
        assertTrue(logFile.getFileName().toString().contains("execution_1_"));
        assertTrue(logFile.getFileName().toString().endsWith(".log"));
    }
}
