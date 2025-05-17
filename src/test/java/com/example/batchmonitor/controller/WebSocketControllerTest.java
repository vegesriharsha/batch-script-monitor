package com.example.batchmonitor.controller;

import com.example.batchmonitor.dto.ConsoleOutput;
import com.example.batchmonitor.dto.ProgressUpdate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

    @InjectMocks
    private WebSocketController webSocketController;

    @Test
    void broadcastProgress_ShouldEchoProgressUpdate() {
        // Arrange
        ProgressUpdate input = ProgressUpdate.builder()
                .executionId(1L)
                .progress(75.0)
                .status("RUNNING")
                .build();

        // Act
        ProgressUpdate result = webSocketController.broadcastProgress(input);

        // Assert
        assertNotNull(result);
        assertEquals(input.getExecutionId(), result.getExecutionId());
        assertEquals(input.getProgress(), result.getProgress());
        assertEquals(input.getStatus(), result.getStatus());
    }

    @Test
    void broadcastConsoleOutput_ShouldEchoConsoleOutput() {
        // Arrange
        ConsoleOutput input = ConsoleOutput.builder()
                .executionId(1L)
                .message("Test console output")
                .timestamp(LocalDateTime.now())
                .type(ConsoleOutput.OutputType.STDOUT)
                .build();

        // Act
        ConsoleOutput result = webSocketController.broadcastConsoleOutput(input);

        // Assert
        assertNotNull(result);
        assertEquals(input.getExecutionId(), result.getExecutionId());
        assertEquals(input.getMessage(), result.getMessage());
        assertEquals(input.getTimestamp(), result.getTimestamp());
        assertEquals(input.getType(), result.getType());
    }
}
