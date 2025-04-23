package com.example.batchmonitor.exception;

public class BatchExecutionException extends RuntimeException {

    public BatchExecutionException(String message) {
        super(message);
    }

    public BatchExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
