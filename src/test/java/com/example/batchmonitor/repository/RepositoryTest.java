package com.example.batchmonitor.repository;

import com.example.batchmonitor.entity.BatchExecution;
import com.example.batchmonitor.entity.ExecutionLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BatchExecutionRepository batchExecutionRepository;

    @Autowired
    private ExecutionLogRepository executionLogRepository;

    @Test
    void testBatchExecutionRepository_FindAllOrdered() {
        // Create test data with different timestamps
        LocalDateTime now = LocalDateTime.now();

        BatchExecution execution1 = BatchExecution.builder()
                .scriptPath("/path/to/script1.sh")
                .parameters("--param1 value1")
                .startTime(now.minusHours(2))
                .status(BatchExecution.ExecutionStatus.COMPLETED)
                .build();

        BatchExecution execution2 = BatchExecution.builder()
                .scriptPath("/path/to/script2.sh")
                .parameters("--param2 value2")
                .startTime(now)
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .build();

        BatchExecution execution3 = BatchExecution.builder()
                .scriptPath("/path/to/script3.sh")
                .parameters("--param3 value3")
                .startTime(now.minusHours(1))
                .status(BatchExecution.ExecutionStatus.FAILED)
                .build();

        // Persist executions
        entityManager.persist(execution1);
        entityManager.persist(execution2);
        entityManager.persist(execution3);
        entityManager.flush();

        // Test findAllByOrderByStartTimeDesc
        List<BatchExecution> result = batchExecutionRepository.findAllByOrderByStartTimeDesc();
        assertEquals(3, result.size());

        // Verify order is by start time descending
        assertEquals(execution2.getId(), result.get(0).getId()); // now
        assertEquals(execution3.getId(), result.get(1).getId()); // now - 1 hour
        assertEquals(execution1.getId(), result.get(2).getId()); // now - 2 hours
    }

    @Test
    void testBatchExecutionRepository_FindByStatus() {
        // Create test data with different statuses
        BatchExecution execution1 = BatchExecution.builder()
                .scriptPath("/path/to/script1.sh")
                .status(BatchExecution.ExecutionStatus.COMPLETED)
                .build();

        BatchExecution execution2 = BatchExecution.builder()
                .scriptPath("/path/to/script2.sh")
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .build();

        BatchExecution execution3 = BatchExecution.builder()
                .scriptPath("/path/to/script3.sh")
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .build();

        // Persist executions
        entityManager.persist(execution1);
        entityManager.persist(execution2);
        entityManager.persist(execution3);
        entityManager.flush();

        // Test findByStatus - RUNNING
        List<BatchExecution> runningExecutions = batchExecutionRepository.findByStatus(BatchExecution.ExecutionStatus.RUNNING);
        assertEquals(2, runningExecutions.size());
        assertTrue(runningExecutions.stream().allMatch(e -> e.getStatus() == BatchExecution.ExecutionStatus.RUNNING));

        // Test findByStatus - COMPLETED
        List<BatchExecution> completedExecutions = batchExecutionRepository.findByStatus(BatchExecution.ExecutionStatus.COMPLETED);
        assertEquals(1, completedExecutions.size());
        assertEquals(execution1.getId(), completedExecutions.get(0).getId());

        // Test findByStatus - FAILED (none should exist)
        List<BatchExecution> failedExecutions = batchExecutionRepository.findByStatus(BatchExecution.ExecutionStatus.FAILED);
        assertTrue(failedExecutions.isEmpty());
    }

    @Test
    void testExecutionLogRepository_FindByBatchExecutionId() {
        // Create parent execution
        BatchExecution execution = BatchExecution.builder()
                .scriptPath("/path/to/script.sh")
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .build();
        entityManager.persist(execution);

        // Create logs for the execution
        LocalDateTime now = LocalDateTime.now();

        ExecutionLog log1 = ExecutionLog.builder()
                .batchExecution(execution)
                .message("Log message 1")
                .timestamp(now.minusMinutes(5))
                .logType(ExecutionLog.LogType.STDOUT)
                .build();

        ExecutionLog log2 = ExecutionLog.builder()
                .batchExecution(execution)
                .message("Log message 2")
                .timestamp(now.minusMinutes(3))
                .logType(ExecutionLog.LogType.STDERR)
                .build();

        ExecutionLog log3 = ExecutionLog.builder()
                .batchExecution(execution)
                .message("Log message 3")
                .timestamp(now.minusMinutes(1))
                .logType(ExecutionLog.LogType.STDOUT)
                .build();

        // Persist logs
        entityManager.persist(log1);
        entityManager.persist(log2);
        entityManager.persist(log3);
        entityManager.flush();

        // Test findByBatchExecutionIdOrderByTimestampAsc
        List<ExecutionLog> logs = executionLogRepository.findByBatchExecutionIdOrderByTimestampAsc(execution.getId());
        assertEquals(3, logs.size());

        // Verify order is by timestamp ascending
        assertEquals(log1.getId(), logs.get(0).getId()); // oldest
        assertEquals(log2.getId(), logs.get(1).getId());
        assertEquals(log3.getId(), logs.get(2).getId()); // newest
    }

    @Test
    void testExecutionLogRepository_FindByBatchExecutionIdAndLogType() {
        // Create parent execution
        BatchExecution execution = BatchExecution.builder()
                .scriptPath("/path/to/script.sh")
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .build();
        entityManager.persist(execution);

        // Create logs of different types
        ExecutionLog stdoutLog1 = ExecutionLog.builder()
                .batchExecution(execution)
                .message("Stdout log 1")
                .timestamp(LocalDateTime.now().minusMinutes(5))
                .logType(ExecutionLog.LogType.STDOUT)
                .build();

        ExecutionLog stderrLog = ExecutionLog.builder()
                .batchExecution(execution)
                .message("Stderr log")
                .timestamp(LocalDateTime.now().minusMinutes(4))
                .logType(ExecutionLog.LogType.STDERR)
                .build();

        ExecutionLog stdoutLog2 = ExecutionLog.builder()
                .batchExecution(execution)
                .message("Stdout log 2")
                .timestamp(LocalDateTime.now().minusMinutes(3))
                .logType(ExecutionLog.LogType.STDOUT)
                .build();

        ExecutionLog systemLog = ExecutionLog.builder()
                .batchExecution(execution)
                .message("System log")
                .timestamp(LocalDateTime.now().minusMinutes(2))
                .logType(ExecutionLog.LogType.SYSTEM)
                .build();

        // Persist logs
        entityManager.persist(stdoutLog1);
        entityManager.persist(stderrLog);
        entityManager.persist(stdoutLog2);
        entityManager.persist(systemLog);
        entityManager.flush();

        // Test findByBatchExecutionIdAndLogTypeOrderByTimestampAsc for STDOUT
        List<ExecutionLog> stdoutLogs = executionLogRepository.findByBatchExecutionIdAndLogTypeOrderByTimestampAsc(
                execution.getId(), ExecutionLog.LogType.STDOUT);

        assertEquals(2, stdoutLogs.size());
        assertEquals("Stdout log 1", stdoutLogs.get(0).getMessage());
        assertEquals("Stdout log 2", stdoutLogs.get(1).getMessage());

        // Test findByBatchExecutionIdAndLogTypeOrderByTimestampAsc for STDERR
        List<ExecutionLog> stderrLogs = executionLogRepository.findByBatchExecutionIdAndLogTypeOrderByTimestampAsc(
                execution.getId(), ExecutionLog.LogType.STDERR);

        assertEquals(1, stderrLogs.size());
        assertEquals("Stderr log", stderrLogs.get(0).getMessage());

        // Test findByBatchExecutionIdAndLogTypeOrderByTimestampAsc for SYSTEM
        List<ExecutionLog> systemLogs = executionLogRepository.findByBatchExecutionIdAndLogTypeOrderByTimestampAsc(
                execution.getId(), ExecutionLog.LogType.SYSTEM);

        assertEquals(1, systemLogs.size());
        assertEquals("System log", systemLogs.get(0).getMessage());
    }

    @Test
    void testBatchExecutionRepository_CascadingOperations() {
        // Create parent execution with logs
        BatchExecution execution = BatchExecution.builder()
                .scriptPath("/path/to/script.sh")
                .status(BatchExecution.ExecutionStatus.COMPLETED)
                .build();

        // Create and associate logs
        ExecutionLog log1 = ExecutionLog.builder()
                .batchExecution(execution)
                .message("Log 1")
                .timestamp(LocalDateTime.now())
                .logType(ExecutionLog.LogType.STDOUT)
                .build();

        ExecutionLog log2 = ExecutionLog.builder()
                .batchExecution(execution)
                .message("Log 2")
                .timestamp(LocalDateTime.now().plusMinutes(1))
                .logType(ExecutionLog.LogType.STDERR)
                .build();

        // Add logs to execution
        execution.getLogs().add(log1);
        execution.getLogs().add(log2);

        // Persist parent (should cascade to logs)
        entityManager.persist(execution);
        entityManager.flush();

        // Verify that both parent and children were persisted
        assertNotNull(execution.getId());
        assertNotNull(log1.getId());
        assertNotNull(log2.getId());

        // Retrieve and verify logs
        List<ExecutionLog> retrievedLogs = executionLogRepository.findByBatchExecutionIdOrderByTimestampAsc(execution.getId());
        assertEquals(2, retrievedLogs.size());
    }
}
