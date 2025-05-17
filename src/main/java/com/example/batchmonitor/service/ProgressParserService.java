package com.example.batchmonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ProgressParserService {

    // Enhanced patterns that support decimal values (e.g., "Progress: 99.9%")
    private static final Pattern[] PROGRESS_PATTERNS = {
            Pattern.compile("\\bprogress:?\\s*(\\d+(\\.\\d+)?)%", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcompleted:?\\s*(\\d+(\\.\\d+)?)%", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(\\d+(\\.\\d+)?)%\\s*complete", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcompleted:?\\s*(\\d+(\\.\\d+)?)/(\\d+(\\.\\d+)?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\btask:?\\s*(\\d+(\\.\\d+)?)\\s*of\\s*(\\d+(\\.\\d+)?)", Pattern.CASE_INSENSITIVE)
    };

    /**
     * Parses progress information from a text line.
     * Supports both integer and decimal percentage values.
     *
     * @param line The text line to parse
     * @return The parsed progress percentage, or null if no progress info found
     */
    public Double parseProgress(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        // Try all patterns
        for (int i = 0; i < PROGRESS_PATTERNS.length; i++) {
            Pattern pattern = PROGRESS_PATTERNS[i];
            Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {
                try {
                    // Direct percentage patterns (first 3 patterns)
                    if (i < 3) {
                        return Double.parseDouble(matcher.group(1));
                    }
                    // Ratio patterns (last 2 patterns)
                    else {
                        double current = Double.parseDouble(matcher.group(1));
                        double total = Double.parseDouble(matcher.group(3));
                        return (current / total) * 100.0;
                    }
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse progress number from: {}", line);
                }
            }
        }

        return null;
    }
}
