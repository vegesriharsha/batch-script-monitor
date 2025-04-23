package com.example.batchmonitor.service;

import com.example.batchmonitor.dto.BatchExecutionRequest;
import com.example.batchmonitor.dto.BatchExecutionResponse;
import com.example.batchmonitor.entity.BatchExecution;
import com.example.batchmonitor.exception.BatchExecutionException;
import com.example.batchmonitor.repository.BatchExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchExecutionService {

    private final BatchExecutionRepository executionRepository;
    private final ScriptExecutionService scriptExecutionService;

    @Value("${batch.scripts.baseDir}")
    private String baseScriptsDir;

    @Value("${batch.scripts.defaultScript}")
    private String defaultScript;

    @Transactional
    public BatchExecutionResponse startExecution(BatchExecutionRequest request) {
        log.info("Starting batch execution for script: {}", request.getScriptName());

        // Build script path
        String scriptPath = resolveScriptPath(request.getScriptName());

        // Create execution record
        BatchExecution execution = BatchExecution.builder()
                .scriptPath(scriptPath)
                .parameters(request.getParameters())
                .status(BatchExecution.ExecutionStatus.PENDING)
                .progress(0.0)
                .build();

        executionRepository.save(execution);

        // Start execution asynchronously
        CompletableFuture<String> future = scriptExecutionService.executeScript(execution);

        // Handle completion (but don't block)
        future.thenAccept(result -> {
            log.info("Script execution completed for ID: {}", execution.getId());
        }).exceptionally(ex -> {
            log.error("Script execution failed for ID: {}", execution.getId(), ex);
            return null;
        });

        return BatchExecutionResponse.fromEntity(execution);
    }

    @Transactional(readOnly = true)
    public BatchExecutionResponse getExecution(Long id) {
        BatchExecution execution = executionRepository.findById(id)
                .orElseThrow(() -> new BatchExecutionException("Execution not found with ID: " + id));

        return BatchExecutionResponse.fromEntity(execution);
    }

    @Transactional(readOnly = true)
    public List<BatchExecutionResponse> getAllExecutions() {
        return executionRepository.findAllByOrderByStartTimeDesc()
                .stream()
                .map(BatchExecutionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private String resolveScriptPath(String scriptName) {
        if (scriptName == null || scriptName.isBlank()) {
            scriptName = defaultScript;
        }

        Path path = Paths.get(scriptName);
        if (!path.isAbsolute()) {
            path = Paths.get(baseScriptsDir, scriptName);
        }

        return path.toString();
    }
}
