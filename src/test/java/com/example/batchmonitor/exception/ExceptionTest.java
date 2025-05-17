package com.example.batchmonitor.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExceptionTest {

    @Test
    void testBatchExecutionException() {
        // Test constructor with message
        BatchExecutionException exception1 = new BatchExecutionException("Test error message");
        assertEquals("Test error message", exception1.getMessage());
        assertNull(exception1.getCause());

        // Test constructor with message and cause
        Exception cause = new RuntimeException("Root cause");
        BatchExecutionException exception2 = new BatchExecutionException("Test with cause", cause);
        assertEquals("Test with cause", exception2.getMessage());
        assertEquals(cause, exception2.getCause());
    }

    @Test
    void testScriptExecutionException() {
        // Test constructor with message and exit code
        ScriptExecutionException exception1 = new ScriptExecutionException("Script failed", 1);
        assertEquals("Script failed", exception1.getMessage());
        assertEquals(Integer.valueOf(1), exception1.getExitCode());
        assertNull(exception1.getCause());

        // Test constructor with message, exit code and cause
        Exception cause = new RuntimeException("Root cause");
        ScriptExecutionException exception2 = new ScriptExecutionException("Script error", 2, cause);
        assertEquals("Script error", exception2.getMessage());
        assertEquals(Integer.valueOf(2), exception2.getExitCode());
        assertEquals(cause, exception2.getCause());
    }

    @Test
    void testGlobalExceptionHandler_BatchExecutionException() {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BatchExecutionException exception = new BatchExecutionException("Batch execution failed");

        // Act
        ResponseEntity<Map<String, String>> response = handler.handleBatchExecutionException(exception);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Batch execution failed", response.getBody().get("error"));
    }

    @Test
    void testGlobalExceptionHandler_ScriptExecutionException() {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ScriptExecutionException exception = new ScriptExecutionException("Script execution failed", 2);

        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleScriptExecutionException(exception);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Script execution failed", response.getBody().get("error"));
        assertEquals(2, response.getBody().get("exitCode"));
    }

    @Test
    void testGlobalExceptionHandler_ValidationException() {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        // Mock MethodArgumentNotValidException with field errors
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError1 = new FieldError("object", "scriptName", "Script name is required");
        FieldError fieldError2 = new FieldError("object", "parameters", "Invalid parameters");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        // Act
        ResponseEntity<Map<String, String>> response = handler.handleValidationException(exception);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Script name is required", response.getBody().get("scriptName"));
        assertEquals("Invalid parameters", response.getBody().get("parameters"));
    }

    @Test
    void testGlobalExceptionHandler_GenericException() {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        RuntimeException exception = new RuntimeException("Something went wrong");

        // Act
        ResponseEntity<Map<String, String>> response = handler.handleGenericException(exception);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("An unexpected error occurred: Something went wrong", response.getBody().get("error"));
    }
}
