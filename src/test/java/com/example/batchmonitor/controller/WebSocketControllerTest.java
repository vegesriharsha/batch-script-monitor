package com.example.batchmonitor.controller;

import com.example.batchmonitor.dto.ConsoleOutput;
import com.example.batchmonitor.dto.ProgressUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketControllerTest {

    @Autowired
    private WebSocketController webSocketController;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void testBroadcastProgress() {
        // Create a progress update
        ProgressUpdate update = ProgressUpdate.builder()
                .executionId(1L)
                .progress(50.0)
                .status("RUNNING")
                .build();

        // Call the controller method
        ProgressUpdate result = webSocketController.broadcastProgress(update);

        // Verify result is the same as input
        assert result == update;
    }

    @Test
    void testBroadcastConsoleOutput() {
        // Create a console output message
        ConsoleOutput output = ConsoleOutput.builder()
                .executionId(1L)
                .message("Test message")
                .timestamp(LocalDateTime.now())
                .type(ConsoleOutput.OutputType.STDOUT)
                .build();

        // Call the controller method
        ConsoleOutput result = webSocketController.broadcastConsoleOutput(output);

        // Verify result is the same as input
        assert result == output;
    }
}
