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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    private final ExecutorService executor = Executors.newCachedThreadPool();

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
                Path outputFile = createTempOutputFile();
                execution.setOutputFilePath(outputFile.toString());
                executionRepository.save(execution);

                // Prepare command
                List<String> command = ScriptUtils.buildCommand(
                        execution.getScriptPath(), execution.getParameters());

                log.debug("Executing command: {}", String.join(" ", command));

                // Start process
                ProcessBuilder processBuilder = new ProcessBuilder(command)
                        .directory(new File(baseScriptsDir));

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

                // Wait for readers to finish
                stdoutReader.stop();
                stderrReader.stop();

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
                updateExecutionFailure(execution, ex.getMessage(), null);
                throw new BatchExecutionException("Error executing script: " + ex.getMessage(), ex);
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

    private Path createTempOutputFile() throws IOException {
        Path tempDir = Files.createTempDirectory("batch-monitor");
        return Files.createFile(tempDir.resolve("output-" + UUID.randomUUID() + ".txt"));
    }

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
