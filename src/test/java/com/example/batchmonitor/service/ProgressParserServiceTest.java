package com.example.batchmonitor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProgressParserServiceTest {

    @InjectMocks
    private ProgressParserService progressParserService;

    @Test
    void testParseProgress_Percentage() {
        // Test various percentage patterns
        assertEquals(45.0, progressParserService.parseProgress("Progress: 45%"));
        assertEquals(30.0, progressParserService.parseProgress("30% complete"));
        assertEquals(75.0, progressParserService.parseProgress("Completed: 75%"));
        assertEquals(10.0, progressParserService.parseProgress("Task is 10% complete"));
    }

    @Test
    void testParseProgress_Ratio() {
        // Test ratio patterns
        assertEquals(50.0, progressParserService.parseProgress("Completed: 50/100"));
        assertEquals(20.0, progressParserService.parseProgress("Task: 2 of 10"));
        assertEquals(66.67, progressParserService.parseProgress("Completed: 2/3"), 0.01);
    }

    @Test
    void testParseProgress_NoMatch() {
        // Test lines with no progress information
        assertNull(progressParserService.parseProgress("Starting process..."));
        assertNull(progressParserService.parseProgress("Task completed successfully"));
        assertNull(progressParserService.parseProgress("Error occurred"));
        assertNull(progressParserService.parseProgress(""));
        assertNull(progressParserService.parseProgress(null));
    }

    @Test
    void testParseProgress_InvalidFormats() {
        // Test invalid formats
        assertNull(progressParserService.parseProgress("Progress: abc%"));
        assertNull(progressParserService.parseProgress("Completed: x/y"));
    }
}
