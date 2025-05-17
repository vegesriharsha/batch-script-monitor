package com.example.batchmonitor.service;

import com.example.batchmonitor.dto.ProgressUpdate;
import com.example.batchmonitor.entity.BatchExecution;
import com.example.batchmonitor.exception.BatchExecutionException;
import com.example.batchmonitor.exception.ScriptExecutionException;
import com.example.batchmonitor.repository.BatchExecutionRepository;
import com.example.batchmonitor.util.ProcessStreamReader;
import com.example.batchmonitor.util.ScriptUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the ScriptExecutionService class.
 * These tests use mocking to avoid actual process execution and are designed
 * to work across different platforms (Windows, Linux, macOS).
 */
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

    @Spy
    @InjectMocks
    private ScriptExecutionService spyScriptExecutionService;

    @TempDir
    Path tempDir;

    private Path logsDir;
    private BatchExecution testExecution;
    private Path scriptFile;
    private Path outputFile;
    private Path logFile;
    private boolean isWindows;

    @BeforeEach
    void setUp() throws IOException {
        // Determine if running on Windows for platform-specific tests
        isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        // Create a temp logs directory
        logsDir = tempDir.resolve("logs");
        Files.createDirectories(logsDir);

        // Configuration via reflection for both service instances
        String tempDirString = tempDir.toString();
        ReflectionTestUtils.setField(scriptExecutionService, "baseScriptsDir", tempDirString);
        ReflectionTestUtils.setField(scriptExecutionService, "executionTimeoutSeconds", 10);
        ReflectionTestUtils.setField(scriptExecutionService, "logsDirectory", logsDir.toString());
        ReflectionTestUtils.setField(scriptExecutionService, "keepLogCopy", true);

        ReflectionTestUtils.setField(spyScriptExecutionService, "baseScriptsDir", tempDirString);
        ReflectionTestUtils.setField(spyScriptExecutionService, "executionTimeoutSeconds", 10);
        ReflectionTestUtils.setField(spyScriptExecutionService, "logsDirectory", logsDir.toString());
        ReflectionTestUtils.setField(spyScriptExecutionService, "keepLogCopy", true);

        // Create a platform-agnostic script file - use .bat on Windows, .sh otherwise
        String scriptName = isWindows ? "test-script.bat" : "test-script.sh";
        String scriptContent = isWindows ?
                "@echo Test script\r\nexit 0" :
                "#!/bin/bash\necho 'Test script'\nexit 0";

        scriptFile = tempDir.resolve(scriptName);
        Files.writeString(scriptFile, scriptContent);
        scriptFile.toFile().setExecutable(true);

        // Create a test output file for test result content
        outputFile = logsDir.resolve("test-output.txt");
        Files.writeString(outputFile, "Test successful output");

        // Create a predefined log file for tests
        logFile = logsDir.resolve("execution_1_20250517_120000.log");
        Files.writeString(logFile, "Execution log content");

        // Create a test execution object
        testExecution = BatchExecution.builder()
                .id(1L)
                .scriptPath(scriptFile.toString())
                .parameters("--param value")
                .status(BatchExecution.ExecutionStatus.PENDING)
                .build();
    }

    /**
     * Test successful script execution flow.
     */
    @Test
    void executeScript_Success() throws Exception {
        // Setup all necessary mocks
        try (MockedStatic<ScriptUtils> scriptUtilsMock = Mockito.mockStatic(ScriptUtils.class)) {
            // Mock the script command building
            List<String> mockCommand = isWindows ?
                    List.of("cmd.exe", "/c", scriptFile.toString(), "--param", "value") :
                    List.of("bash", scriptFile.toString(), "--param", "value");

            scriptUtilsMock.when(() -> ScriptUtils.buildCommand(anyString(), anyString()))
                    .thenReturn(mockCommand);

            // Mock reading file content - specifically from our log file
            scriptUtilsMock.when(() -> ScriptUtils.readFileContent(logFile.toString()))
                    .thenReturn("Test successful output");

            // Mock the process execution
            ProcessBuilder mockProcessBuilder = mock(ProcessBuilder.class);
            Process mockProcess = mock(Process.class);

            // Mock process I/O streams
            ByteArrayInputStream stdoutStream = new ByteArrayInputStream(
                    "Test output\nProgress: 50%\nTest complete".getBytes(StandardCharsets.UTF_8));
            ByteArrayInputStream stderrStream = new ByteArrayInputStream(
                    new byte[0]); // Empty stream for stderr

            when(mockProcess.getInputStream()).thenReturn(stdoutStream);
            when(mockProcess.getErrorStream()).thenReturn(stderrStream);
            when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
            when(mockProcess.exitValue()).thenReturn(0); // Success exit code

            when(mockProcessBuilder.directory(any(File.class))).thenReturn(mockProcessBuilder);
            when(mockProcessBuilder.start()).thenReturn(mockProcess);

            // Mock the process creation and log file creation
            doReturn(mockProcessBuilder).when(spyScriptExecutionService).createProcessBuilder(anyList());
            doReturn(logFile).when(spyScriptExecutionService).createLogFile(any(BatchExecution.class));

            // Act - Execute the script
            CompletableFuture<String> future = spyScriptExecutionService.executeScript(testExecution);
            String result = future.get(); // Wait for completion

            // Assert - Verify expected results and interactions
            assertEquals("Test successful output", result, "Result should match mock content");
            assertEquals(BatchExecution.ExecutionStatus.COMPLETED, testExecution.getStatus(),
                    "Execution status should be COMPLETED");
            assertEquals(0, testExecution.getExitCode(), "Exit code should be 0");
            assertNotNull(testExecution.getEndTime(), "End time should be set");
            assertEquals(logFile.toString(), testExecution.getOutputFilePath(),
                    "Output file path should match our mock log file");

            // Verify repository interactions
            verify(executionRepository, atLeastOnce()).save(same(testExecution));

            // Verify websocket notifications
            verify(webSocketService).sendStatusUpdate(1L, "RUNNING");
            verify(webSocketService).sendStatusUpdate(1L, "COMPLETED");
            verify(webSocketService).sendProgressUpdate(argThat(update ->
                    update.getExecutionId() == 1L &&
                            update.getProgress() == 100.0 &&
                            "COMPLETED".equals(update.getStatus())));

            // Verify output processing
            verify(consoleOutputService, atLeastOnce()).processStandardOutput(eq(testExecution), anyString());
            verify(consoleOutputService).logSystemMessage(same(testExecution),
                    contains("Script execution completed successfully"));
        }
    }

    /**
     * Test script execution failure flow.
     */
    @Test
    void executeScript_ProcessFails() throws Exception {
        // Setup mocks for failure scenario
        try (MockedStatic<ScriptUtils> scriptUtilsMock = Mockito.mockStatic(ScriptUtils.class)) {
            // Mock the script command building
            List<String> mockCommand = isWindows ?
                    List.of("cmd.exe", "/c", scriptFile.toString(), "--param", "value") :
                    List.of("bash", scriptFile.toString(), "--param", "value");

            scriptUtilsMock.when(() -> ScriptUtils.buildCommand(anyString(), anyString()))
                    .thenReturn(mockCommand);

            // Mock process execution
            ProcessBuilder mockProcessBuilder = mock(ProcessBuilder.class);
            Process mockProcess = mock(Process.class);

            // Mock process I/O streams with error output
            ByteArrayInputStream stdoutStream = new ByteArrayInputStream(
                    "Test failed output".getBytes(StandardCharsets.UTF_8));
            ByteArrayInputStream stderrStream = new ByteArrayInputStream(
                    "Error: process failed".getBytes(StandardCharsets.UTF_8));

            when(mockProcess.getInputStream()).thenReturn(stdoutStream);
            when(mockProcess.getErrorStream()).thenReturn(stderrStream);
            when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
            when(mockProcess.exitValue()).thenReturn(1); // Failure exit code

            when(mockProcessBuilder.directory(any(File.class))).thenReturn(mockProcessBuilder);
            when(mockProcessBuilder.start()).thenReturn(mockProcess);

            // Mock the process creation and log file creation
            doReturn(mockProcessBuilder).when(spyScriptExecutionService).createProcessBuilder(anyList());
            doReturn(logFile).when(spyScriptExecutionService).createLogFile(any(BatchExecution.class));

            // Act - Execute should throw an exception due to non-zero exit code
            CompletableFuture<String> future = spyScriptExecutionService.executeScript(testExecution);

            // Assert - Verify exception and failure state
            ExecutionException exception = assertThrows(ExecutionException.class,
                    () -> future.get(),
                    "Future.get() should throw ExecutionException");

            assertTrue(exception.getCause() instanceof ScriptExecutionException,
                    "Exception cause should be ScriptExecutionException, but was: " +
                            (exception.getCause() != null ? exception.getCause().getClass().getName() : "null"));

            ScriptExecutionException scriptException = (ScriptExecutionException) exception.getCause();
            assertEquals(1, scriptException.getExitCode(), "Exit code should be 1");

            // Verify execution state
            assertEquals(BatchExecution.ExecutionStatus.FAILED, testExecution.getStatus(),
                    "Execution status should be FAILED");
            assertEquals(1, testExecution.getExitCode(), "Exit code should be 1");
            assertNotNull(testExecution.getEndTime(), "End time should be set");
            assertNotNull(testExecution.getErrorMessage(), "Error message should be set");

            // Verify repository and notification interactions
            verify(executionRepository, atLeastOnce()).save(same(testExecution));
            verify(webSocketService).sendStatusUpdate(1L, "RUNNING");
            verify(webSocketService).sendStatusUpdate(1L, "FAILED");

            // Verify output and error processing
            verify(consoleOutputService, atLeastOnce()).processStandardOutput(eq(testExecution), anyString());
            verify(consoleOutputService, atLeastOnce()).processErrorOutput(eq(testExecution), anyString());
            verify(consoleOutputService).logSystemMessage(same(testExecution), contains("Script execution failed"));
        }
    }

    /**
     * Test execution timeout handling.
     */
    @Test
    void executeScript_Timeout() throws Exception {
        // Setup mocks for timeout scenario
        try (MockedStatic<ScriptUtils> scriptUtilsMock = Mockito.mockStatic(ScriptUtils.class)) {
            // Mock the script command building
            List<String> mockCommand = isWindows ?
                    List.of("cmd.exe", "/c", scriptFile.toString(), "--param", "value") :
                    List.of("bash", scriptFile.toString(), "--param", "value");

            scriptUtilsMock.when(() -> ScriptUtils.buildCommand(anyString(), anyString()))
                    .thenReturn(mockCommand);

            // Mock process execution with timeout
            ProcessBuilder mockProcessBuilder = mock(ProcessBuilder.class);
            Process mockProcess = mock(Process.class);

            // Mock streams
            ByteArrayInputStream stdoutStream = new ByteArrayInputStream(
                    "Process running...".getBytes(StandardCharsets.UTF_8));
            ByteArrayInputStream stderrStream = new ByteArrayInputStream(new byte[0]);

            when(mockProcess.getInputStream()).thenReturn(stdoutStream);
            when(mockProcess.getErrorStream()).thenReturn(stderrStream);

            // Simulate timeout by returning false from waitFor
            when(mockProcess.waitFor(anyLong(), any())).thenReturn(false);

            when(mockProcessBuilder.directory(any(File.class))).thenReturn(mockProcessBuilder);
            when(mockProcessBuilder.start()).thenReturn(mockProcess);

            // Mock process creation and destruction
            doReturn(mockProcessBuilder).when(spyScriptExecutionService).createProcessBuilder(anyList());
            doReturn(logFile).when(spyScriptExecutionService).createLogFile(any(BatchExecution.class));
            doNothing().when(mockProcess).destroyForcibly();

            // Act - Execute should throw timeout exception
            CompletableFuture<String> future = spyScriptExecutionService.executeScript(testExecution);

            // Assert - Verify exception and timeout handling
            ExecutionException exception = assertThrows(ExecutionException.class, () -> future.get());
            assertTrue(exception.getCause() instanceof BatchExecutionException);
            assertTrue(exception.getCause().getMessage().contains("timed out"));

            // Verify process was destroyed
            verify(mockProcess).destroyForcibly();

            // Verify execution was marked as failed with appropriate error
            verify(executionRepository, atLeastOnce()).save(same(testExecution));
            verify(webSocketService).sendStatusUpdate(1L, "RUNNING");
            verify(webSocketService).sendStatusUpdate(1L, "FAILED");
        }
    }

    /**
     * Test for process interruption handling.
     */
    @Test
    void executeScript_ProcessInterrupted() throws Exception {
        // Setup mocks for interruption scenario
        try (MockedStatic<ScriptUtils> scriptUtilsMock = Mockito.mockStatic(ScriptUtils.class)) {
            scriptUtilsMock.when(() -> ScriptUtils.buildCommand(anyString(), anyString()))
                    .thenReturn(List.of(scriptFile.toString()));

            ProcessBuilder mockProcessBuilder = mock(ProcessBuilder.class);
            Process mockProcess = mock(Process.class);

            // Mock standard streams
            ByteArrayInputStream stdoutStream = new ByteArrayInputStream(
                    "Processing...".getBytes(StandardCharsets.UTF_8));
            ByteArrayInputStream stderrStream = new ByteArrayInputStream(new byte[0]);

            when(mockProcess.getInputStream()).thenReturn(stdoutStream);
            when(mockProcess.getErrorStream()).thenReturn(stderrStream);

            // Simulate interruption by throwing InterruptedException
            when(mockProcess.waitFor(anyLong(), any())).thenThrow(new InterruptedException("Test interruption"));

            when(mockProcessBuilder.directory(any(File.class))).thenReturn(mockProcessBuilder);
            when(mockProcessBuilder.start()).thenReturn(mockProcess);

            doReturn(mockProcessBuilder).when(spyScriptExecutionService).createProcessBuilder(anyList());
            doReturn(logFile).when(spyScriptExecutionService).createLogFile(any(BatchExecution.class));

            // Act - Execute will be interrupted
            CompletableFuture<String> future = spyScriptExecutionService.executeScript(testExecution);

            // Assert - Verify exception and interruption handling
            ExecutionException exception = assertThrows(ExecutionException.class, () -> future.get());
            assertTrue(exception.getCause() instanceof BatchExecutionException);
            assertTrue(exception.getCause().getMessage().contains("interrupted"));

            // Verify execution was marked as failed with appropriate error
            assertEquals(BatchExecution.ExecutionStatus.FAILED, testExecution.getStatus());
            assertNotNull(testExecution.getErrorMessage());
            assertTrue(testExecution.getErrorMessage().contains("interrupted"));

            verify(executionRepository, atLeastOnce()).save(same(testExecution));
            verify(webSocketService).sendStatusUpdate(1L, "RUNNING");
            verify(webSocketService).sendStatusUpdate(1L, "FAILED");
            verify(consoleOutputService).logSystemMessage(same(testExecution), contains("interrupted"));
        }
    }

    /**
     * Test for updateExecutionSuccess method.
     */
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
        assertEquals(BatchExecution.ExecutionStatus.COMPLETED, execution.getStatus(),
                "Status should be updated to COMPLETED");
        assertEquals(100.0, execution.getProgress(), "Progress should be 100%");
        assertNotNull(execution.getEndTime(), "End time should be set");

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

    /**
     * Test for updateExecutionFailure method.
     */
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
        assertEquals(BatchExecution.ExecutionStatus.FAILED, execution.getStatus(),
                "Status should be updated to FAILED");
        assertEquals(errorMessage, execution.getErrorMessage(), "Error message should be set");
        assertEquals(exitCode, execution.getExitCode(), "Exit code should be set");
        assertNotNull(execution.getEndTime(), "End time should be set");

        verify(executionRepository).save(execution);
        verify(consoleOutputService).logSystemMessage(execution, "Script execution failed: " + errorMessage);
        verify(webSocketService).sendStatusUpdate(1L, "FAILED");
    }

    /**
     * Test for createLogFile method.
     */
    @Test
    void createLogFile_ShouldCreateValidFile() throws Exception {
        // Arrange
        BatchExecution execution = BatchExecution.builder()
                .id(1L)
                .build();

        // Act - directly call the protected method
        Path logFile = spyScriptExecutionService.createLogFile(execution);

        // Assert
        assertNotNull(logFile, "Log file should not be null");
        assertTrue(Files.exists(logFile), "Log file should exist");
        assertTrue(logFile.getFileName().toString().contains("execution_1_"),
                "Log file name should contain execution ID");
        assertTrue(logFile.getFileName().toString().endsWith(".log"),
                "Log file should have .log extension");
    }
}
