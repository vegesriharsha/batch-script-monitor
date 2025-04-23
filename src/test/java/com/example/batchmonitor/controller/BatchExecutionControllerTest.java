package com.example.batchmonitor.controller;

import com.example.batchmonitor.dto.BatchExecutionRequest;
import com.example.batchmonitor.dto.BatchExecutionResponse;
import com.example.batchmonitor.dto.ConsoleOutput;
import com.example.batchmonitor.entity.BatchExecution;
import com.example.batchmonitor.service.BatchExecutionService;
import com.example.batchmonitor.service.ConsoleOutputService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchExecutionController.class)
public class BatchExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BatchExecutionService batchExecutionService;

    @MockBean
    private ConsoleOutputService consoleOutputService;

    private BatchExecutionResponse mockResponse;
    private List<BatchExecutionResponse> mockResponses;
    private List<ConsoleOutput> mockConsoleOutputs;

    @BeforeEach
    void setUp() {
        // Setup mock response
        mockResponse = BatchExecutionResponse.builder()
                .id(1L)
                .scriptPath("/path/to/script.sh")
                .parameters("--param1 value1")
                .startTime(LocalDateTime.now())
                .status(BatchExecution.ExecutionStatus.RUNNING)
                .progress(25.0)
                .build();

        mockResponses = List.of(
                mockResponse,
                BatchExecutionResponse.builder()
                        .id(2L)
                        .scriptPath("/path/to/another.sh")
                        .startTime(LocalDateTime.now().minusHours(1))
                        .endTime(LocalDateTime.now().minusMinutes(30))
                        .status(BatchExecution.ExecutionStatus.COMPLETED)
                        .progress(100.0)
                        .build()
        );

        mockConsoleOutputs = List.of(
                ConsoleOutput.builder()
                        .executionId(1L)
                        .message("Starting process...")
                        .timestamp(LocalDateTime.now())
                        .type(ConsoleOutput.OutputType.STDOUT)
                        .build(),
                ConsoleOutput.builder()
                        .executionId(1L)
                        .message("Process 25% complete")
                        .timestamp(LocalDateTime.now())
                        .type(ConsoleOutput.OutputType.STDOUT)
                        .build()
        );

        // Setup mock service methods
        when(batchExecutionService.startExecution(any(BatchExecutionRequest.class)))
                .thenReturn(mockResponse);
        when(batchExecutionService.getExecution(anyLong()))
                .thenReturn(mockResponse);
        when(batchExecutionService.getAllExecutions())
                .thenReturn(mockResponses);
        when(consoleOutputService.getConsoleOutput(anyLong()))
                .thenReturn(mockConsoleOutputs);
    }

    @Test
    void testStartExecution() throws Exception {
        BatchExecutionRequest request = new BatchExecutionRequest();
        request.setScriptName("script.sh");
        request.setParameters("--param1 value1");

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.scriptPath").value("/path/to/script.sh"))
                .andExpect(jsonPath("$.parameters").value("--param1 value1"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.progress").value(25.0));
    }

    @Test
    void testGetExecution() throws Exception {
        mockMvc.perform(get("/api/executions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.scriptPath").value("/path/to/script.sh"))
                .andExpect(jsonPath("$.parameters").value("--param1 value1"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.progress").value(25.0));
    }

    @Test
    void testGetAllExecutions() throws Exception {
        mockMvc.perform(get("/api/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    void testGetConsoleOutput() throws Exception {
        mockMvc.perform(get("/api/executions/1/console"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].executionId").value(1L))
                .andExpect(jsonPath("$[0].message").value("Starting process..."))
                .andExpect(jsonPath("$[0].type").value("STDOUT"));
    }
}
