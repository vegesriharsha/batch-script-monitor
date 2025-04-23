package com.example.batchmonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ProgressParserService {

    // Example patterns: "Progress: 45%", "Completed: 45/100", "Task 45% complete"
    private static final Pattern[] PROGRESS_PATTERNS = {
            Pattern.compile("\\bprogress:?\\s*(\\d+)%", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcompleted:?\\s*(\\d+)%", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(\\d+)%\\s*complete", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcompleted:?\\s*(\\d+)/(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\btask:?\\s*(\\d+)\\s*of\\s*(\\d+)", Pattern.CASE_INSENSITIVE)
    };

    public Double parseProgress(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        // Try all patterns
        for (Pattern pattern : PROGRESS_PATTERNS) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                try {
                    if (matcher.groupCount() == 1) {
                        // Direct percentage
                        return Double.parseDouble(matcher.group(1));
                    } else if (matcher.groupCount() == 2) {
                        // Ratio (e.g., 45/100)
                        double current = Double.parseDouble(matcher.group(1));
                        double total = Double.parseDouble(matcher.group(2));
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
