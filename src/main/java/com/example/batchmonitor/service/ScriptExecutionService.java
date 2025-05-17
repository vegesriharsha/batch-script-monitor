package com.example.batchmonitor.service;

import com.example.batchmonitor.dto.ProgressUpdate;
import com.example.batchmonitor.entity.BatchExecution;
import com.example.batchmonitor.exception.BatchExecutionException;
import com.example.batchmonitor.exception.ScriptExecutionException;
import com.example.batchmonitor.repository.BatchExecutionRepository;
import com.example.batchmonitor.util.ProcessStreamReader;
import com.example.batchmonitor.util.ScriptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for executing batch scripts and monitoring their execution.
 * Provides methods to start script execution, monitor progress, and handle completion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScriptExecutionService {

    private final BatchExecutionRepository executionRepository;
    private final ConsoleOutputService consoleOutputService;
    private final WebSocketService webSocketService;

    @Value("${batch.scripts.baseDir}")
    private String baseScriptsDir;

    @Value("${batch.execution.timeout}")
    private int executionTimeoutSeconds;

    @Value("${batch.execution.logs.directory}")
    private String logsDirectory;

    @Value("${batch.execution.logs.keepCopy:false}")
    private boolean keepLogCopy;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Executes a batch script asynchronously.
     * Updates the execution status, captures output, and monitors progress.
     *
     * @param execution The batch execution entity
     * @return A CompletableFuture containing the script result
     */
    @Transactional
    public CompletableFuture<String> executeScript(BatchExecution execution) {
        log.info("Executing script: {} with parameters: {}",
                execution.getScriptPath(), execution.getParameters());

        // Update execution status to RUNNING
        execution.setStatus(BatchExecution.ExecutionStatus.RUNNING);
        execution.setStartTime(LocalDateTime.now());
        executionRepository.save(execution);

        // Notify clients about status change
        webSocketService.sendStatusUpdate(execution.getId(), execution.getStatus().name());

        return CompletableFuture.supplyAsync(() -> {
            Process process = null;
            ProcessStreamReader stdoutReader = null;
            ProcessStreamReader stderrReader = null;

            try {
                // Create temp file for output if needed
                Path outputFile = createLogFile(execution);
                execution.setOutputFilePath(outputFile.toString());
                executionRepository.save(execution);

                // Prepare command
                List<String> command = ScriptUtils.buildCommand(
                        execution.getScriptPath(), execution.getParameters());

                log.debug("Executing command: {}", String.join(" ", command));

                // Start process
                ProcessBuilder processBuilder = createProcessBuilder(command);
                processBuilder.directory(new File(baseScriptsDir));

                process = processBuilder.start();

                // Set up process output readers
                stdoutReader = new ProcessStreamReader(
                        process.getInputStream(),
                        line -> consoleOutputService.processStandardOutput(execution, line));

                stderrReader = new ProcessStreamReader(
                        process.getErrorStream(),
                        line -> consoleOutputService.processErrorOutput(execution, line));

                // Start readers
                stdoutReader.start();
                stderrReader.start();

                // Wait for process to complete with timeout
                boolean completed = process.waitFor(executionTimeoutSeconds, TimeUnit.SECONDS);

                if (!completed) {
                    process.destroyForcibly();
                    throw new BatchExecutionException("Script execution timed out after "
                            + executionTimeoutSeconds + " seconds");
                }

                // Wait for readers to finish to ensure all output is processed
                if (stdoutReader != null) {
                    stdoutReader.stop();
                    try {
                        stdoutReader.waitFor();
                    } catch (InterruptedException e) {
                        log.warn("Interrupted while waiting for stdout reader to finish", e);
                        Thread.currentThread().interrupt();
                    }
                }

                if (stderrReader != null) {
                    stderrReader.stop();
                    try {
                        stderrReader.waitFor();
                    } catch (InterruptedException e) {
                        log.warn("Interrupted while waiting for stderr reader to finish", e);
                        Thread.currentThread().interrupt();
                    }
                }

                // Check exit code
                int exitCode = process.exitValue();
                execution.setExitCode(exitCode);

                if (exitCode != 0) {
                    throw new ScriptExecutionException(
                            "Script execution failed with exit code: " + exitCode,
                            exitCode);
                }

                // Read output file content
                String result = ScriptUtils.readFileContent(outputFile.toString());

                // Update execution record
                updateExecutionSuccess(execution);

                return result;

            } catch (ScriptExecutionException ex) {
                updateExecutionFailure(execution, ex.getMessage(), ex.getExitCode());
                throw ex;
            } catch (InterruptedException ex) {
                updateExecutionFailure(execution, "Script execution was interrupted", null);
                Thread.currentThread().interrupt();
                throw new BatchExecutionException("Script execution was interrupted", ex);
            } catch (Exception ex) {
                String errorMsg = "Error executing script: " + ex.getMessage();
                log.error(errorMsg, ex);
                updateExecutionFailure(execution, errorMsg, null);
                throw new BatchExecutionException(errorMsg, ex);
            } finally {
                // Ensure process and readers are closed
                if (process != null) {
                    process.destroyForcibly();
                }
                if (stdoutReader != null) {
                    stdoutReader.stop();
                }
                if (stderrReader != null) {
                    stderrReader.stop();
                }
            }
        }, executor);
    }

    /**
     * Creates a ProcessBuilder for the given command.
     * Extracted as a method to allow mocking in tests.
     *
     * @param command The command to execute
     * @return A ProcessBuilder configured with the command
     */
    ProcessBuilder createProcessBuilder(List<String> command) {
        return new ProcessBuilder(command);
    }

    /**
     * Creates a log file for the batch execution output.
     * Extracted as a protected method to allow mocking in tests.
     *
     * @param execution The batch execution entity
     * @return The path to the created log file
     * @throws IOException If an error occurs creating the file
     */
    protected Path createLogFile(BatchExecution execution) throws IOException {
        // Create logs directory if it doesn't exist
        Path logsDir = Paths.get(logsDirectory);
        if (!Files.exists(logsDir)) {
            Files.createDirectories(logsDir);
        }

        // Generate log file name based on execution ID and timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("execution_%d_%s.log", execution.getId(), timestamp);

        Path logFile = logsDir.resolve(fileName);
        log.debug("Creating log file: {}", logFile);
        return Files.createFile(logFile);
    }

    /**
     * Updates the execution status to successful completion.
     *
     * @param execution The batch execution entity to update
     */
    @Transactional
    public void updateExecutionSuccess(BatchExecution execution) {
        execution.setStatus(BatchExecution.ExecutionStatus.COMPLETED);
        execution.setEndTime(LocalDateTime.now());
        execution.setProgress(100.0);
        executionRepository.save(execution);

        // Log system message
        consoleOutputService.logSystemMessage(execution, "Script execution completed successfully");

        // Notify clients
        webSocketService.sendStatusUpdate(execution.getId(), execution.getStatus().name());
        webSocketService.sendProgressUpdate(
                ProgressUpdate.builder()
                        .executionId(execution.getId())
                        .progress(100.0)
                        .status(execution.getStatus().name())
                        .build()
        );
    }

    /**
     * Updates the execution status to failure.
     *
     * @param execution The batch execution entity to update
     * @param errorMessage The error message to set
     * @param exitCode The exit code to set (may be null)
     */
    @Transactional
    public void updateExecutionFailure(BatchExecution execution, String errorMessage, Integer exitCode) {
        execution.setStatus(BatchExecution.ExecutionStatus.FAILED);
        execution.setEndTime(LocalDateTime.now());
        execution.setErrorMessage(errorMessage);
        if (exitCode != null) {
            execution.setExitCode(exitCode);
        }
        executionRepository.save(execution);

        // Log system message
        consoleOutputService.logSystemMessage(execution, "Script execution failed: " + errorMessage);

        // Notify clients
        webSocketService.sendStatusUpdate(execution.getId(), execution.getStatus().name());
    }
}
