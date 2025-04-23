package com.example.batchmonitor.exception;

public class ScriptExecutionException extends BatchExecutionException {

    private final Integer exitCode;

    public ScriptExecutionException(String message, Integer exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public ScriptExecutionException(String message, Integer exitCode, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public Integer getExitCode() {
        return exitCode;
    }
}
