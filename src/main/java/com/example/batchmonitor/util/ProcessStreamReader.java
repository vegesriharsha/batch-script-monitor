package com.example.batchmonitor.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public class ProcessStreamReader implements Runnable {

    private final InputStream inputStream;
    private final Consumer<String> outputConsumer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Thread thread;

    public ProcessStreamReader(InputStream inputStream, Consumer<String> outputConsumer) {
        this.inputStream = inputStream;
        this.outputConsumer = outputConsumer;
        this.thread = new Thread(this);
    }

    public void start() {
        running.set(true);
        thread.start();
    }

    public void stop() {
        running.set(false);
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                outputConsumer.accept(line);
            }
        } catch (IOException e) {
            if (running.get()) {  // Only log if we weren't deliberately stopped
                log.error("Error reading from process stream", e);
            }
        }
    }

    public void waitFor() throws InterruptedException {
        if (thread != null && thread.isAlive()) {
            thread.join();
        }
    }
}
