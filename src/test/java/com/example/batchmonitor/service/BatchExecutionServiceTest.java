package com.example.batchmonitor.service;

import com.example.batchmonitor.dto.BatchExecutionRequest;
import com.example.batchmonitor.dto.BatchExecutionResponse;
import com.example.batchmonitor.entity.BatchExecution;
import com.example.batchmonitor.exception.BatchExecutionException;
import com.example.batchmonitor.repository.BatchExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchExecutionServiceTest {

    @Mock
    private BatchExecutionRepository executionRepository;

    @Mock
    private ScriptExecutionService scriptExecutionService;

    @InjectMocks
    private BatchExecutionService batchExecutionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(batchExecutionService, "baseScriptsDir", "/test/scripts");
        ReflectionTestUtils.setField(batchExecutionService, "defaultScript", "default.sh");
    }

    @Test
    void startExecution_ShouldCreateAndSaveExecution() {
        // Arrange
        BatchExecutionRequest request = new BatchExecutionRequest();
        request.setScriptName("test-script.sh");
        request.setParameters("--param value");

        BatchExecution savedExecution = BatchExecution.builder()
                .id(1L)
                .scriptPath(new File("/test/scripts/test-script.sh").getPath())
                .parameters("--param value")
                .status(BatchExecution.ExecutionStatus.PENDING)
                .progress(0.0)
                .build();

        when(executionRepository.save(any(BatchExecution.class))).thenReturn(savedExecution);
        when(scriptExecutionService.executeScript(any(BatchExecution.class)))
                .thenReturn(CompletableFuture.completedFuture("Success"));

        // Act
        BatchExecutionResponse response = batchExecutionService.startExecution(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(new File("/test/scripts/test-script.sh").getPath(), response.getScriptPath());
        assertEquals("--param value", response.getParameters());
        assertEquals(BatchExecution.ExecutionStatus.PENDING, response.getStatus());

        // Verify repository interaction
        ArgumentCaptor<BatchExecution> executionCaptor = ArgumentCaptor.forClass(BatchExecution.class);
        verify(executionRepository).save(executionCaptor.capture());

        BatchExecution capturedExecution = executionCaptor.getValue();
        assertEquals(new File("/test/scripts/test-script.sh").getPath(), capturedExecution.getScriptPath());
        assertEquals("--param value", capturedExecution.getParameters());
        assertEquals(BatchExecution.ExecutionStatus.PENDING, capturedExecution.getStatus());
        assertEquals(0.0, capturedExecution.getProgress());

        // Verify script execution was started
        verify(scriptExecutionService).executeScript(any(BatchExecution.class));
    }

    @Test
    void startExecution_WithDefaultScript_ShouldUseDefaultScript() {
        // Arrange
        BatchExecutionRequest request = new BatchExecutionRequest();
        request.setScriptName("");  // Empty script name should use default
        request.setParameters("--param value");

        BatchExecution savedExecution = BatchExecution.builder()
                .id(1L)
                .scriptPath(new File("/test/scripts/default.sh").getPath())  // Default script path
                .parameters("--param value")
                .status(BatchExecution.ExecutionStatus.PENDING)
                .progress(0.0)
                .build();

        when(executionRepository.save(any(BatchExecution.class))).thenReturn(savedExecution);
        when(scriptExecutionService.executeScript(any(BatchExecution.class)))
                .thenReturn(CompletableFuture.completedFuture("Success"));

        // Act
        BatchExecutionResponse response = batchExecutionService.startExecution(request);

        // Assert
        assertEquals(new File("/test/scripts/default.sh").getPath(), response.getScriptPath());

        // Verify repository interaction
        ArgumentCaptor<BatchExecution> executionCaptor = ArgumentCaptor.forClass(BatchExecution.class);
        verify(executionRepository).save(executionCaptor.capture());

        BatchExecution capturedExecution = executionCaptor.getValue();
        assertEquals(new File("/test/scripts/default.sh").getPath(), capturedExecution.getScriptPath());
    }

    @Test
    void startExecution_WithAbsolutePath_ShouldUseAbsolutePath() {
        // Arrange
        String absolutePath = new File("/absolute/path/script.sh").getAbsolutePath();
        BatchExecutionRequest request = new BatchExecutionRequest();
        request.setScriptName(absolutePath);
        request.setParameters("--param value");

        BatchExecution savedExecution = BatchExecution.builder()
                .id(1L)
                .scriptPath(absolutePath)  // Absolute path is used directly
                .parameters("--param value")
                .status(BatchExecution.ExecutionStatus.PENDING)
                .progress(0.0)
                .build();

        when(executionRepository.save(any(BatchExecution.class))).thenReturn(savedExecution);
        when(scriptExecutionService.executeScript(any(BatchExecution.class)))
                .thenReturn(CompletableFuture.completedFuture("Success"));

        // Act
        BatchExecutionResponse response = batchExecutionService.startExecution(request);

        // Assert
        assertEquals(absolutePath, response.getScriptPath());

        // Verify repository interaction
        ArgumentCaptor<BatchExecution> executionCaptor = ArgumentCaptor.forClass(BatchExecution.class);
        verify(executionRepository).save(executionCaptor.capture());

        BatchExecution capturedExecution = executionCaptor.getValue();
        assertEquals(absolutePath, capturedExecution.getScriptPath());
    }

    @Test
    void getExecution_ShouldReturnExecution() {
        // Arrange
        BatchExecution execution = BatchExecution.builder()
                .id(1L)
                .scriptPath("/test/script.sh")
                .parameters("--param value")
                .startTime(LocalDateTime.now())
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .progress(50.0)
                .build();

        when(executionRepository.findById(1L)).thenReturn(Optional.of(execution));

        // Act
        BatchExecutionResponse response = batchExecutionService.getExecution(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("/test/script.sh", response.getScriptPath());
        assertEquals("--param value", response.getParameters());
        assertEquals(execution.getStartTime(), response.getStartTime());
        assertEquals(BatchExecution.ExecutionStatus.RUNNING, response.getStatus());
        assertEquals(50.0, response.getProgress());

        verify(executionRepository).findById(1L);
    }

    @Test
    void getExecution_WhenNotFound_ShouldThrowException() {
        // Arrange
        when(executionRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BatchExecutionException.class, () -> {
            batchExecutionService.getExecution(99L);
        });

        verify(executionRepository).findById(99L);
    }

    @Test
    void getAllExecutions_ShouldReturnAllExecutions() {
        // Arrange
        List<BatchExecution> executions = Arrays.asList(
                BatchExecution.builder()
                        .id(1L)
                        .scriptPath("/test/script1.sh")
                        .status(BatchExecution.ExecutionStatus.COMPLETED)
                        .startTime(LocalDateTime.now().minusHours(1))
                        .endTime(LocalDateTime.now())
                        .build(),
                BatchExecution.builder()
                        .id(2L)
                        .scriptPath("/test/script2.sh")
                        .status(BatchExecution.ExecutionStatus.RUNNING)
                        .startTime(LocalDateTime.now())
                        .build()
        );

        when(executionRepository.findAllByOrderByStartTimeDesc()).thenReturn(executions);

        // Act
        List<BatchExecutionResponse> responses = batchExecutionService.getAllExecutions();

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());

        assertEquals(1L, responses.get(0).getId());
        assertEquals("/test/script1.sh", responses.get(0).getScriptPath());
        assertEquals(BatchExecution.ExecutionStatus.COMPLETED, responses.get(0).getStatus());

        assertEquals(2L, responses.get(1).getId());
        assertEquals("/test/script2.sh", responses.get(1).getScriptPath());
        assertEquals(BatchExecution.ExecutionStatus.RUNNING, responses.get(1).getStatus());

        verify(executionRepository).findAllByOrderByStartTimeDesc();
    }
}
