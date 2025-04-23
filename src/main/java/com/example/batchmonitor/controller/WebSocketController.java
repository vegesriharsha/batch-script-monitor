package com.example.batchmonitor.controller;

import com.example.batchmonitor.dto.ConsoleOutput;
import com.example.batchmonitor.dto.ProgressUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    @MessageMapping("/progress")
    @SendTo("/topic/progress")
    public ProgressUpdate broadcastProgress(ProgressUpdate progress) {
        return progress;
    }

    @MessageMapping("/console")
    @SendTo("/topic/console-output")
    public ConsoleOutput broadcastConsoleOutput(ConsoleOutput output) {
        return output;
    }
}
