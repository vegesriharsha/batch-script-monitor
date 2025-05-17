package com.example.batchmonitor.controller;

import com.example.batchmonitor.dto.BatchExecutionRequest;
import com.example.batchmonitor.dto.BatchExecutionResponse;
import com.example.batchmonitor.dto.ConsoleOutput;
import com.example.batchmonitor.entity.BatchExecution;
import com.example.batchmonitor.service.BatchExecutionService;
import com.example.batchmonitor.service.ConsoleOutputService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BatchExecutionControllerTest {

    @Mock
    private BatchExecutionService batchExecutionService;

    @Mock
    private ConsoleOutputService consoleOutputService;

    @InjectMocks
    private BatchExecutionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    void startExecution_ShouldReturnCreatedExecution() throws Exception {
        // Arrange
        BatchExecutionRequest request = new BatchExecutionRequest();
        request.setScriptName("test-script.txt");
        request.setParameters("--delay 2");

        BatchExecutionResponse response = BatchExecutionResponse.builder()
                .id(1L)
                .scriptPath("/test-script.txt")
                .parameters("--delay 2")
                .status(BatchExecution.ExecutionStatus.PENDING)
                .progress(0.0)
                .build();

        when(batchExecutionService.startExecution(any(BatchExecutionRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scriptName\":\"test-script.txt\",\"parameters\":\"--delay 2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.scriptPath", is("/test-script.txt")))
                .andExpect(jsonPath("$.parameters", is("--delay 2")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.progress", is(0.0)));

        verify(batchExecutionService).startExecution(any(BatchExecutionRequest.class));
    }

    @Test
    void getExecution_ShouldReturnExecutionById() throws Exception {
        // Arrange
        BatchExecutionResponse response = BatchExecutionResponse.builder()
                .id(1L)
                .scriptPath("/test-script.txt")
                .parameters("--delay 2")
                .startTime(LocalDateTime.now())
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .progress(50.0)
                .build();

        when(batchExecutionService.getExecution(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/executions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("RUNNING")))
                .andExpect(jsonPath("$.progress", is(50.0)));

        verify(batchExecutionService).getExecution(1L);
    }

    @Test
    void getAllExecutions_ShouldReturnAllExecutions() throws Exception {
        // Arrange
        List<BatchExecutionResponse> executions = Arrays.asList(
                BatchExecutionResponse.builder().id(1L).scriptPath("/script1.sh").status(BatchExecution.ExecutionStatus.COMPLETED).build(),
                BatchExecutionResponse.builder().id(2L).scriptPath("/script2.sh").status(BatchExecution.ExecutionStatus.RUNNING).build()
        );

        when(batchExecutionService.getAllExecutions()).thenReturn(executions);

        // Act & Assert
        mockMvc.perform(get("/api/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].status", is("COMPLETED")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].status", is("RUNNING")));

        verify(batchExecutionService).getAllExecutions();
    }

    @Test
    void getConsoleOutput_ShouldReturnOutputForExecution() throws Exception {
        // Arrange
        List<ConsoleOutput> outputs = Arrays.asList(
                ConsoleOutput.builder()
                        .executionId(1L)
                        .message("Starting execution...")
                        .type(ConsoleOutput.OutputType.STDOUT)
                        .timestamp(LocalDateTime.now())
                        .build(),
                ConsoleOutput.builder()
                        .executionId(1L)
                        .message("Error during execution")
                        .type(ConsoleOutput.OutputType.STDERR)
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        when(consoleOutputService.getConsoleOutput(1L)).thenReturn(outputs);

        // Act & Assert
        mockMvc.perform(get("/api/executions/1/console"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].message", is("Starting execution...")))
                .andExpect(jsonPath("$[0].type", is("STDOUT")))
                .andExpect(jsonPath("$[1].message", is("Error during execution")))
                .andExpect(jsonPath("$[1].type", is("STDERR")));

        verify(consoleOutputService).getConsoleOutput(1L);
    }
}
