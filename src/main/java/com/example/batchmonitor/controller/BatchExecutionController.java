package com.example.batchmonitor.controller;

import com.example.batchmonitor.dto.BatchExecutionRequest;
import com.example.batchmonitor.dto.BatchExecutionResponse;
import com.example.batchmonitor.dto.ConsoleOutput;
import com.example.batchmonitor.service.BatchExecutionService;
import com.example.batchmonitor.service.ConsoleOutputService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
@Slf4j
public class BatchExecutionController {

    private final BatchExecutionService batchExecutionService;
    private final ConsoleOutputService consoleOutputService;

    @PostMapping
    public ResponseEntity<BatchExecutionResponse> startExecution(
            @Valid @RequestBody BatchExecutionRequest request) {
        log.info("REST request to start batch execution: {}", request);
        return ResponseEntity.ok(batchExecutionService.startExecution(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BatchExecutionResponse> getExecution(@PathVariable Long id) {
        log.info("REST request to get batch execution: {}", id);
        return ResponseEntity.ok(batchExecutionService.getExecution(id));
    }

    @GetMapping
    public ResponseEntity<List<BatchExecutionResponse>> getAllExecutions() {
        log.info("REST request to get all batch executions");
        return ResponseEntity.ok(batchExecutionService.getAllExecutions());
    }

    @GetMapping("/{id}/console")
    public ResponseEntity<List<ConsoleOutput>> getConsoleOutput(@PathVariable Long id) {
        log.info("REST request to get console output for execution: {}", id);
        return ResponseEntity.ok(consoleOutputService.getConsoleOutput(id));
    }
}
