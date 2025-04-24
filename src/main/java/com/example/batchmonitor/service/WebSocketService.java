package com.example.batchmonitor.service;

import com.example.batchmonitor.dto.ConsoleOutput;
import com.example.batchmonitor.dto.ProgressUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    @Value("${batch.websocket.topic.progress}")
    private String progressTopic;

    @Value("${batch.websocket.topic.console}")
    private String consoleTopic;

    @Value("${batch.websocket.topic.status}")
    private String statusTopic;

    // Create a many-unicast sink that allows multiple subscribers but only emits to subscribers who were active at the time of emission
    private final Sinks.Many<ConsoleOutput> consoleOutputSink = Sinks.many().multicast().onBackpressureBuffer();

    public void sendProgressUpdate(ProgressUpdate update) {
        log.debug("Sending progress update: {}", update);
        messagingTemplate.convertAndSend(progressTopic, update);
    }

    public void sendStatusUpdate(Long executionId, String status) {
        log.debug("Sending status update for execution {}: {}", executionId, status);
        messagingTemplate.convertAndSend(statusTopic,
                Map.of("executionId", executionId, "static", status));
    }

    public void sendConsoleOutput(ConsoleOutput output) {
        log.debug("Sending console output for execution {}: {} ({})",
                output.getExecutionId(),
                output.getMessage(),
                output.getType());

        // Send to WebSocket topic
        messagingTemplate.convertAndSend(consoleTopic, output);

        // Push to reactive stream with appropriate error handling
        Sinks.EmitResult result = consoleOutputSink.tryEmitNext(output);
        if (result.isFailure()) {
            log.warn("Failed to emit console output to sink: {}", result);
        }
    }

    public Flux<ConsoleOutput> getConsoleOutputStream() {
        return consoleOutputSink.asFlux();
    }
}
