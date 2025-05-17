package com.example.batchmonitor.service;

import com.example.batchmonitor.dto.ConsoleOutput;
import com.example.batchmonitor.dto.ProgressUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketService webSocketService;

    @BeforeEach
    void setUp() {
        // Set topic paths via reflection
        ReflectionTestUtils.setField(webSocketService, "progressTopic", "/topic/progress");
        ReflectionTestUtils.setField(webSocketService, "consoleTopic", "/topic/console-output");
        ReflectionTestUtils.setField(webSocketService, "statusTopic", "/topic/status");
    }

    @Test
    void testSendProgressUpdate() {
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
        // Call service method
        webSocketService.sendStatusUpdate(1L, "COMPLETED");

        // Verify interaction with messaging template
        verify(messagingTemplate).convertAndSend(
                eq("/topic/status"),
                eq(Map.of("executionId", 1L, "status", "COMPLETED")));
    }

    @Test
    void testSendConsoleOutput() {
        // Create test data
        LocalDateTime now = LocalDateTime.now();
        ConsoleOutput output = ConsoleOutput.builder()
                .executionId(1L)
                .message("Test message")
                .timestamp(now)
                .type(ConsoleOutput.OutputType.STDOUT)
                .build();

        // Call service method
        webSocketService.sendConsoleOutput(output);

        // Verify interaction with messaging template
        verify(messagingTemplate).convertAndSend(eq("/topic/console-output"), eq(output));
    }

    @Test
    void testGetConsoleOutputStream() {
        // Create test data
        ConsoleOutput output1 = ConsoleOutput.builder()
                .executionId(1L)
                .message("Test message 1")
                .timestamp(LocalDateTime.now())
                .type(ConsoleOutput.OutputType.STDOUT)
                .build();

        ConsoleOutput output2 = ConsoleOutput.builder()
                .executionId(1L)
                .message("Test message 2")
                .timestamp(LocalDateTime.now().plusSeconds(1))
                .type(ConsoleOutput.OutputType.STDERR)
                .build();

        // Get the reactive stream
        Flux<ConsoleOutput> outputStream = webSocketService.getConsoleOutputStream();

        // Set up the StepVerifier
        StepVerifier.FirstStep<ConsoleOutput> verifier = StepVerifier.create(outputStream);

        // Send console outputs
        webSocketService.sendConsoleOutput(output1);
        webSocketService.sendConsoleOutput(output2);

        // Verify the stream contains both outputs
        verifier
                .expectNext(output1)
                .expectNext(output2)
                .thenCancel()
                .verify();
    }
}
