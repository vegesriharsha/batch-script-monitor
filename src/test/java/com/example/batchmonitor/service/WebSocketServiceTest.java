package com.example.batchmonitor.service;

import com.example.batchmonitor.dto.ConsoleOutput;
import com.example.batchmonitor.dto.ProgressUpdate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketService webSocketService;

    @Test
    void testSendProgressUpdate() {
        // Set topic paths via reflection
        ReflectionTestUtils.setField(webSocketService, "progressTopic", "/topic/progress");

        // Create test data
        ProgressUpdate update = ProgressUpdate.builder()
                .executionId(1L)
                .progress(50.0)
                .status("RUNNING")
                .build();

        // Call service method
        webSocketService.sendProgressUpdate(update);

        // Verify interaction with messaging template
        verify(messagingTemplate).convertAndSend(eq("/topic/progress"), eq(update));
    }

    @Test
    void testSendStatusUpdate() {
        // Set topic paths via reflection
        ReflectionTestUtils.setField(webSocketService, "statusTopic", "/topic/status");

        // Call service method
        webSocketService.sendStatusUpdate(1L, "COMPLETED");

        // Verify interaction with messaging template
        verify(messagingTemplate).convertAndSend(
                eq("/topic/status"),
                eq(Map.of("executionId", 1L, "status", "COMPLETED")));
    }

    @Test
    void testSendConsoleOutput() {
        // Set topic paths via reflection
        ReflectionTestUtils.setField(webSocketService, "consoleTopic", "/topic/console-output");

        // Create test data
        ConsoleOutput output = ConsoleOutput.builder()
                .executionId(1L)
                .message("Test message")
                .timestamp(LocalDateTime.now())
                .type(ConsoleOutput.OutputType.STDOUT)
                .build();

        // Call service method
        webSocketService.sendConsoleOutput(output);

        // Verify interaction with messaging template
        verify(messagingTemplate).convertAndSend(eq("/topic/console-output"), eq(output));
    }
}
