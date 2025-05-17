package com.example.batchmonitor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProgressParserServiceTest {

    @Test
    void testProgressPattern() {
        // This pattern would match both integer and decimal percentages
        Pattern decimalPattern = Pattern.compile("\\bprogress:?\\s*(\\d+(\\.\\d+)?)%", Pattern.CASE_INSENSITIVE);

        // Test with integer percentage
        Matcher intMatcher = decimalPattern.matcher("Progress: 45%");
        assertTrue(intMatcher.find());
        assertEquals("45", intMatcher.group(1));

        // Test with decimal percentage
        Matcher decimalMatcher = decimalPattern.matcher("Progress: 99.9%");
        assertTrue(decimalMatcher.find());
        assertEquals("99.9", decimalMatcher.group(1));

        // Converting to double
        double percentage = Double.parseDouble(decimalMatcher.group(1));
        assertEquals(99.9, percentage, 0.001);
    }

    @Test
    void testRatioPattern() {
        // Enhanced ratio pattern to support decimals in both numerator and denominator
        Pattern ratioPattern = Pattern.compile(
                "\\bcompleted:?\\s*(\\d+(\\.\\d+)?)/(\\d+(\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE);

        // Test with integer ratio
        Matcher intMatcher = ratioPattern.matcher("Completed: 20/100");
        assertTrue(intMatcher.find());
        assertEquals("20", intMatcher.group(1));
        assertEquals("100", intMatcher.group(3));

        // Test with decimal numerator
        Matcher decimalMatcher = ratioPattern.matcher("Completed: 0.5/1");
        assertTrue(decimalMatcher.find());
        assertEquals("0.5", decimalMatcher.group(1));
        assertEquals("1", decimalMatcher.group(3));

        // Test with both decimal values
        Matcher bothDecimalMatcher = ratioPattern.matcher("Completed: 0.75/1.5");
        assertTrue(bothDecimalMatcher.find());
        assertEquals("0.75", bothDecimalMatcher.group(1));
        assertEquals("1.5", bothDecimalMatcher.group(3));

        // Converting to double and calculating percentage
        double current = Double.parseDouble(bothDecimalMatcher.group(1));
        double total = Double.parseDouble(bothDecimalMatcher.group(3));
        double percentage = (current / total) * 100.0;
        assertEquals(50.0, percentage, 0.001);
    }

    /**
     * This test demonstrates how all the patterns in ProgressParserService
     * could be updated to support decimal values.
     */
    @Test
    void testPatterns() {
        // All enhanced patterns (modified copies from ProgressParserService)
        Pattern[] enhancedPatterns = {
                Pattern.compile("\\bprogress:?\\s*(\\d+(\\.\\d+)?)%", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\bcompleted:?\\s*(\\d+(\\.\\d+)?)%", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\b(\\d+(\\.\\d+)?)%\\s*complete", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\bcompleted:?\\s*(\\d+(\\.\\d+)?)/(\\d+(\\.\\d+)?)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\btask:?\\s*(\\d+(\\.\\d+)?)\\s*of\\s*(\\d+(\\.\\d+)?)", Pattern.CASE_INSENSITIVE)
        };

        // Test cases with different formats including decimals
        String[] testCases = {
                "Progress: 99.9%",
                "Completed: 75.5%",
                "95.5% complete",
                "Completed: 2.5/5",
                "Task: 0.5 of 1.0"
        };

        // Expected percentage values
        double[] expected = {
                99.9,
                75.5,
                95.5,
                50.0,
                50.0
        };

        // Test each case with the corresponding pattern
        for (int i = 0; i < testCases.length; i++) {
            String testCase = testCases[i];
            Pattern pattern = enhancedPatterns[i];

            Matcher matcher = pattern.matcher(testCase);
            assertTrue(matcher.find(), "Pattern should match: " + testCase);

            double result;
            if (i < 3) {
                // Direct percentage patterns
                result = Double.parseDouble(matcher.group(1));
            } else {
                // Ratio patterns
                double current = Double.parseDouble(matcher.group(1));
                double total = Double.parseDouble(matcher.group(3));
                result = (current / total) * 100.0;
            }

            assertEquals(expected[i], result, 0.001, "Result for: " + testCase);
        }
    }
}
