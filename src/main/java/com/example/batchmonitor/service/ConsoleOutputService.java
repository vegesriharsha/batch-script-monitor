package com.example.batchmonitor.service;

import com.example.batchmonitor.dto.ConsoleOutput;
import com.example.batchmonitor.dto.ProgressUpdate;
import com.example.batchmonitor.entity.BatchExecution;
import com.example.batchmonitor.entity.ExecutionLog;
import com.example.batchmonitor.repository.ExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsoleOutputService {

    private final ExecutionLogRepository logRepository;
    private final WebSocketService webSocketService;
    private final ProgressParserService progressParserService;

    @Transactional
    public void processStandardOutput(BatchExecution execution, String line) {
        log.debug("Processing stdout for execution {}: {}", execution.getId(), line);

        // Create and save log entry
        ExecutionLog logEntry = ExecutionLog.builder()
                .batchExecution(execution)
                .message(line)
                .timestamp(LocalDateTime.now())
                .logType(ExecutionLog.LogType.STDOUT)
                .build();

        logRepository.save(logEntry);

        // Send to WebSocket
        webSocketService.sendConsoleOutput(ConsoleOutput.fromLog(logEntry));

        // Check for progress updates
        Double progress = progressParserService.parseProgress(line);
        if (progress != null) {
            execution.setProgress(progress);
            webSocketService.sendProgressUpdate(
                    ProgressUpdate.builder()
                            .executionId(execution.getId())
                            .progress(progress)
                            .status(execution.getStatus().name())
                            .build()
            );
        }
    }

    @Transactional
    public void processErrorOutput(BatchExecution execution, String line) {
        log.debug("Processing stderr for execution {}: {}", execution.getId(), line);

        // Create and save log entry
        ExecutionLog logEntry = ExecutionLog.builder()
                .batchExecution(execution)
                .message(line)
                .timestamp(LocalDateTime.now())
                .logType(ExecutionLog.LogType.STDERR)
                .build();

        logRepository.save(logEntry);

        // Send to WebSocket
        webSocketService.sendConsoleOutput(ConsoleOutput.fromLog(logEntry));
    }

    @Transactional
    public void logSystemMessage(BatchExecution execution, String message) {
        log.debug("Logging system message for execution {}: {}", execution.getId(), message);

        ExecutionLog logEntry = ExecutionLog.builder()
                .batchExecution(execution)
                .message(message)
                .timestamp(LocalDateTime.now())
                .logType(ExecutionLog.LogType.SYSTEM)
                .build();

        logRepository.save(logEntry);
    }

    @Transactional(readOnly = true)
    public List<ConsoleOutput> getConsoleOutput(Long executionId) {
        return logRepository.findByBatchExecutionIdOrderByTimestampAsc(executionId)
                .stream()
                .filter(log -> log.getLogType() != ExecutionLog.LogType.SYSTEM)
                .map(ConsoleOutput::fromLog)
                .collect(Collectors.toList());
    }
}
